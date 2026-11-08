package com.resumeapp.data.prefs

import com.resumeapp.billing.model.SubscriptionStatus
import com.resumeapp.domain.model.SubscriptionTier
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface SubscriptionPrefs {
    val currentStatus: SubscriptionStatus
    val statusFlow: Flow<SubscriptionStatus>

    suspend fun saveSubscriptionStatus(status: SubscriptionStatus)
    suspend fun getSubscriptionStatus(): SubscriptionStatus?
    suspend fun clearSubscriptionStatus()
}

class SubscriptionPrefsImpl @Inject constructor(
    private val userPrefs: UserPrefs
) : SubscriptionPrefs {

    private val _status = MutableStateFlow<SubscriptionStatus?>(null)
    override val statusFlow: Flow<SubscriptionStatus> = _status
        .map { it ?: SubscriptionStatus(isActive = false) }
        .distinctUntilChanged()

    override val currentStatus: SubscriptionStatus
        get() = _status.value ?: SubscriptionStatus(isActive = false)

    override suspend fun saveSubscriptionStatus(status: SubscriptionStatus) {
        _status.value = status
        // Persist to DataStore via UserPrefs
        userPrefs.updateSubscription(
            tier = when {
                status.sku == "lifetime" -> SubscriptionTier.LIFETIME
                status.sku == "pro_monthly" -> SubscriptionTier.PRO
                else -> SubscriptionTier.FREE
            },
            expiryDate = status.expiryTime,
            purchaseToken = status.purchaseToken
        )
    }

    override suspend fun getSubscriptionStatus(): SubscriptionStatus? = _status.value

    override suspend fun clearSubscriptionStatus() {
        _status.value = null
        userPrefs.updateSubscription(
            tier = SubscriptionTier.FREE,
            expiryDate = null,
            purchaseToken = null
        )
    }

    init {
        // Load from UserPrefs on init
        val prefsStatus = userPrefs.subscriptionStatusFlow.firstOrNull()
        if (prefsStatus != null) {
            _status.value = SubscriptionStatus(
                isActive = prefsStatus.isActive,
                sku = when (prefsStatus.tier) {
                    SubscriptionTier.LIFETIME -> "lifetime"
                    SubscriptionTier.PRO -> "pro_monthly"
                    else -> null
                },
                purchaseToken = prefsStatus.purchaseToken,
                expiryTime = prefsStatus.expiryDate
            )
        }
    }
}