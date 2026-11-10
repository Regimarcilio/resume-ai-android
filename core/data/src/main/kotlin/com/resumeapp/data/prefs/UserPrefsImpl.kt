package com.resumeapp.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.getString
import androidx.datastore.preferences.core.getLong
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.DataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.resumeapp.domain.model.ConsentPurpose
import com.resumeapp.domain.model.ConsentRecord
import com.resumeapp.domain.model.SubscriptionStatus
import com.resumeapp.domain.model.SubscriptionTier
import com.resumeapp.domain.model.FeatureFlag
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import java.lang.reflect.Type

/**
 * User Preferences Interface - Abstracts DataStore implementation
 */
interface UserPrefs {
    val userId: String
    val subscriptionTier: SubscriptionTier
    val subscriptionExpiryDate: Long?
    val purchaseToken: String?
    val preferredTemplateId: String?
    val aiUsageCount: Int
    val lastSyncAt: Long
    val consentVersion: String?
    val consentTimestamp: Long?
    val consentAccepted: Boolean
    val consentPurposes: Set<String> // Stored as JSON string
    val consentPolicyHash: String?

    suspend fun setUserId(userId: String)
    suspend fun updateSubscription(tier: SubscriptionTier, expiryDate: Long?, purchaseToken: String?)
    suspend fun setPreferredTemplateId(templateId: String?)
    suspend fun incrementAIUsage()
    suspend fun setLastSyncAt(timestamp: Long)
    suspend fun updateConsent(record: com.resumeapp.domain.model.ConsentRecord)
    suspend fun clearConsent()
    suspend fun clearAll()
    
    // Flows for reactive UI
    val userIdFlow: Flow<String>
    val subscriptionStatusFlow: Flow<com.resumeapp.domain.model.SubscriptionStatus>
    val consentRecordFlow: Flow<com.resumeapp.domain.model.ConsentRecord?>
}

/**
 * DataStore Implementation of UserPrefs
 */
class UserPrefsImpl constructor(
    private val context: Context,
    private val dataStore: androidx.datastore.preferences.DataStore<Preferences>,
    private val masterKey: MasterKey
) : UserPrefs {

    // Keys
    private val USER_ID_KEY = stringPreferencesKey("user_id")
    private val SUBSCRIPTION_TIER_KEY = stringPreferencesKey("subscription_tier")
    private val SUBSCRIPTION_EXPIRY_KEY = longPreferencesKey("subscription_expiry")
    private val PURCHASE_TOKEN_KEY = stringPreferencesKey("purchase_token")
    private val PREFERRED_TEMPLATE_ID_KEY = stringPreferencesKey("preferred_template_id")
    private val AI_USAGE_COUNT_KEY = longPreferencesKey("ai_usage_count")
    private val LAST_SYNC_AT_KEY = longPreferencesKey("last_sync_at")
    private val CONSENT_VERSION_KEY = stringPreferencesKey("consent_version")
    private val CONSENT_TIMESTAMP_KEY = longPreferencesKey("consent_timestamp")
    private val CONSENT_ACCEPTED_KEY = longPreferencesKey("consent_accepted") // 0/1
    private val CONSENT_PURPOSES_KEY = stringPreferencesKey("consent_purposes")
    private val CONSENT_POLICY_HASH_KEY = stringPreferencesKey("consent_policy_hash")

    private val gson = Gson()

    override val userId: String
        get() = runBlocking { dataStore.data.map { it.getString(USER_ID_KEY) ?: "" }.first() }

    override val subscriptionTier: SubscriptionTier
        get() = runBlocking {
            val tierStr = dataStore.data.map { it.getString(SUBSCRIPTION_TIER_KEY) ?: SubscriptionTier.FREE.name }.first()
            SubscriptionTier.valueOf(tierStr)
        }

    override val subscriptionExpiryDate: Long?
        get() = runBlocking {
            val expiry = dataStore.data.map { it.getLong(SUBSCRIPTION_EXPIRY_KEY) }.first()
            if (expiry > 0) expiry else null
        }

    override val purchaseToken: String?
        get() = runBlocking { dataStore.data.map { it.getString(PURCHASE_TOKEN_KEY) }.first() }

    override val preferredTemplateId: String?
        get() = runBlocking { dataStore.data.map { it.getString(PREFERRED_TEMPLATE_ID_KEY) }.first() }

    override val aiUsageCount: Int
        get() = runBlocking { dataStore.data.map { it.getLong(AI_USAGE_COUNT_KEY)?.toInt() ?: 0 }.first() }

    override val lastSyncAt: Long
        get() = runBlocking { dataStore.data.map { it.getLong(LAST_SYNC_AT_KEY) ?: 0L }.first() }

    override val consentVersion: String?
        get() = runBlocking { dataStore.data.map { it.getString(CONSENT_VERSION_KEY) }.first() }

    override val consentTimestamp: Long?
        get() = runBlocking {
            val ts = dataStore.data.map { it.getLong(CONSENT_TIMESTAMP_KEY) }.first()
            if (ts > 0) ts else null
        }

    override val consentAccepted: Boolean
        get() = runBlocking { dataStore.data.map { it.getLong(CONSENT_ACCEPTED_KEY) == 1L }.first() }

    override val consentPurposes: Set<String>
        get() = runBlocking {
            val json = dataStore.data.map { it.getString(CONSENT_PURPOSES_KEY) ?: "[]" }.first()
            val type = object : TypeToken<Set<String>>() {}.type
            try {
                Gson().fromJson(json, type)
            } catch (e: Exception) {
                emptySet()
            }
        }

    override val consentPolicyHash: String?
        get() = runBlocking { dataStore.data.map { it.getString(CONSENT_POLICY_HASH_KEY) }.first() }

    // Flows
    override val userIdFlow: Flow<String> = dataStore.data
        .map { it.getString(USER_ID_KEY) ?: "" }
        .distinctUntilChanged()

    override val subscriptionStatusFlow: Flow<com.resumeapp.domain.model.SubscriptionStatus> = dataStore.data
        .map { prefs ->
            val tier = SubscriptionTier.valueOf(prefs.getString(SUBSCRIPTION_TIER_KEY) ?: SubscriptionTier.FREE.name)
            val expiry = prefs.getLong(SUBSCRIPTION_EXPIRY_KEY)
            val token = prefs.getString(PURCHASE_TOKEN_KEY)
            val isActive = when {
                tier == com.resumeapp.domain.model.SubscriptionTier.FREE -> true
                expiry > 0 -> System.currentTimeMillis() < expiry
                else -> true // Lifetime
            }
            val features = getFeaturesForTier(tier)
            com.resumeapp.domain.model.SubscriptionStatus(
                tier = tier,
                expiryDate = if (expiry > 0) expiry else null,
                isActive = isActive,
                purchaseToken = prefs.getString(PURCHASE_TOKEN_KEY),
                features = features
            )
        }
        .distinctUntilChanged()

    override val consentRecordFlow: Flow<com.resumeapp.domain.model.ConsentRecord?> = dataStore.data
        .map { prefs ->
            val accepted = prefs.getLong(CONSENT_ACCEPTED_KEY) == 1L
            if (!accepted) null
            else {
                val purposesJson = prefs.getString(CONSENT_PURPOSES_KEY) ?: "[]"
                val purposes = try {
                    val type = object : TypeToken<Set<String>>() {}.type
                    Gson().fromJson(purposesJson, type)
                } catch (e: Exception) {
                    emptySet<String>()
                }
                com.resumeapp.domain.model.ConsentRecord(
                    version = prefs.getString(CONSENT_VERSION_KEY) ?: "",
                    timestamp = prefs.getLong(CONSENT_TIMESTAMP_KEY),
                    policyHash = prefs.getString(CONSENT_POLICY_HASH_KEY) ?: "",
                    accepted = accepted,
                    purposes = purposes.map { ConsentPurpose.valueOf(it) }.toSet()
                )
            }
        }
        .distinctUntilChanged()
        .catch { }

    override suspend fun setUserId(userId: String) {
        dataStore.edit { it.putString(USER_ID_KEY, userId) }
    }

    override suspend fun updateSubscription(tier: SubscriptionTier, expiryDate: Long?, purchaseToken: String?) {
        dataStore.edit { prefs ->
            prefs.putString(SUBSCRIPTION_TIER_KEY, tier.name)
            expiryDate?.let { prefs.putLong(SUBSCRIPTION_EXPIRY_KEY, it) }
            purchaseToken?.let { prefs.putString(PURCHASE_TOKEN_KEY, it) }
        }
    }

    override suspend fun setPreferredTemplateId(templateId: String?) {
        dataStore.edit { prefs ->
            templateId?.let { prefs.putString(PREFERRED_TEMPLATE_ID_KEY, it) }
                ?: prefs.remove(PREFERRED_TEMPLATE_ID_KEY)
        }
    }

    override suspend fun incrementAIUsage() {
        dataStore.edit { prefs ->
            val current = prefs.getLong(AI_USAGE_COUNT_KEY)
            prefs.putLong(AI_USAGE_COUNT_KEY, current + 1)
        }
    }

    override suspend fun setLastSyncAt(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs.putLong(LAST_SYNC_AT_KEY, timestamp)
        }
    }

    override suspend fun updateConsent(record: com.resumeapp.domain.model.ConsentRecord) {
        dataStore.edit { prefs ->
            prefs.putString(CONSENT_VERSION_KEY, record.version)
            prefs.putLong(CONSENT_TIMESTAMP_KEY, record.timestamp)
            prefs.putLong(CONSENT_ACCEPTED_KEY, if (record.accepted) 1L else 0L)
            prefs.putString(CONSENT_PURPOSES_KEY, Gson().toJson(record.purposes.map { it.name }))
            prefs.putString(CONSENT_POLICY_HASH_KEY, record.policyHash)
        }
    }

    override suspend fun clearConsent() {
        dataStore.edit { prefs ->
            prefs.putLong(CONSENT_ACCEPTED_KEY, 0L)
            prefs.remove(CONSENT_VERSION_KEY)
            prefs.remove(CONSENT_TIMESTAMP_KEY)
            prefs.remove(CONSENT_PURPOSES_KEY)
            prefs.remove(CONSENT_POLICY_HASH_KEY)
        }
    }

    override suspend fun clearAll() {
        dataStore.edit { prefs ->
            prefs.remove(USER_ID_KEY)
            prefs.putString(SUBSCRIPTION_TIER_KEY, com.resumeapp.domain.model.SubscriptionTier.FREE.name)
            prefs.remove(SUBSCRIPTION_EXPIRY_KEY)
            prefs.remove(PURCHASE_TOKEN_KEY)
            prefs.remove(PREFERRED_TEMPLATE_ID_KEY)
            prefs.putLong(AI_USAGE_COUNT_KEY, 0L)
            prefs.remove(LAST_SYNC_AT_KEY)
            prefs.putLong(CONSENT_ACCEPTED_KEY, 0L)
            prefs.remove(CONSENT_VERSION_KEY)
            prefs.remove(CONSENT_TIMESTAMP_KEY)
            prefs.remove(CONSENT_PURPOSES_KEY)
            prefs.remove(CONSENT_POLICY_HASH_KEY)
        }
    }

    private fun getFeaturesForTier(tier: com.resumeapp.domain.model.SubscriptionTier): Set<com.resumeapp.domain.model.FeatureFlag> {
        return when (tier) {
            com.resumeapp.domain.model.SubscriptionTier.FREE -> setOf()
            com.resumeapp.domain.model.SubscriptionTier.PRO,
            com.resumeapp.domain.model.SubscriptionTier.LIFETIME -> setOf(
                com.resumeapp.domain.model.FeatureFlag.UNLIMITED_RESUMES,
                com.resumeapp.domain.model.FeatureFlag.UNLIMITED_AI_ASSIST,
                com.resumeapp.domain.model.FeatureFlag.ATS_FULL,
                com.resumeapp.domain.model.FeatureFlag.PREMIUM_TEMPLATES,
                com.resumeapp.domain.model.FeatureFlag.NO_WATERMARK
            )
            com.resumeapp.domain.model.SubscriptionTier.ENTERPRISE -> com.resumeapp.domain.model.FeatureFlag.values().toSet()
        }
    }
}