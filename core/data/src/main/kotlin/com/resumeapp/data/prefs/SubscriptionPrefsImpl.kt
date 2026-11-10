package com.resumeapp.data.prefs

import com.resumeapp.domain.model.SubscriptionStatus
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

class SubscriptionPrefsImpl constructor(
    private val userPrefs: UserPrefs
) : SubscriptionPrefs {

    private val _status = MutableStateFlow<SubscriptionStatus?>(null)
    override val statusFlow: Flow<SubscriptionStatus> = _status
        .map { it ?: SubscriptionStatus() }
        .distinctUntilChanged()

    override val currentStatus: SubscriptionStatus
        get() = _status.value ?: SubscriptionStatus()

    override suspend fun saveSubscriptionStatus(status: SubscriptionStatus) {
        _status.value = status
        // Persist to DataStore via UserPrefs
        userPrefs.updateSubscription(
            tier = status.tier,
            expiryDate = status.expiryDate,
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
                tier = prefsStatus.tier,
                expiryDate = prefsStatus.expiryDate,
                isActive = prefsStatus.isActive,
                purchaseToken = prefsStatus.purchaseToken
            )
        }
    }
}