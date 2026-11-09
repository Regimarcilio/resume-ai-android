package com.resumeapp.domain.util

object Constants {
    // PDF Service
    const val PDF_SERVICE_BASE_URL = "http://10.0.2.2:8082"
    
    // AI Gateway
    const val AI_GATEWAY_BASE_URL = "http://10.0.2.2:8081"
    
    // Cache TTLs
    const val PDF_CACHE_TTL_DAYS = 30
    const val AI_CACHE_TTL_SECONDS = 3600
    const val TRACE_RETENTION_DAYS = 90
    
    // App
    const val APP_NAME = "Resume AI Builder"
    const val PACKAGE_NAME = "com.resumeapp"
    
    // Play Billing
    const val PRO_MONTHLY_SKU = "pro_monthly"
    const val LIFETIME_SKU = "lifetime"
    
    // Analytics
    const val ANALYTICS_ENABLED_DEFAULT = false
    const val CRASH_REPORTING_ENABLED_DEFAULT = true
}