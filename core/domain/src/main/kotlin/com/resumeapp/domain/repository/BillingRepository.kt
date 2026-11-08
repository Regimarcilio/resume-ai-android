package com.resumeapp.domain.repository

import com.resumeapp.billing.model.PurchaseResult
import com.resumeapp.billing.model.SubscriptionPlan
import com.resumeapp.billing.model.SubscriptionStatus
import kotlinx.coroutines.flow.StateFlow

interface BillingRepository {
    suspend fun getAvailablePlans(): Result<List<SubscriptionPlan>>
    
    suspend fun purchasePlan(activity: android.app.Activity, sku: String): Result<PurchaseResult>
    
    suspend fun restorePurchases(): Result<SubscriptionStatus>
    
    suspend fun getCurrentStatus(): Result<SubscriptionStatus>
    
    suspend fun acknowledgePurchase(purchaseToken: String): Result<Unit>
    
    val subscriptionStatus: StateFlow<SubscriptionStatus>
    
    fun isProUser(): Boolean
    
    fun isLifetimeUser(): Boolean
    
    fun isMonthlyProUser(): Boolean
}