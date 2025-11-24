package com.example.dubaothoitiet.data

/**
 * Đại diện cho một preference update đang chờ sync khi offline
 * Validates: Requirements 11.5, 15.4
 */
sealed class PendingPreferenceUpdate {
    abstract val timestamp: Long
    abstract val userId: Int
    
    /**
     * Update notification preferences
     */
    data class NotificationPreferencesUpdate(
        override val userId: Int,
        val preferences: NotificationPreferences,
        override val timestamp: Long = System.currentTimeMillis()
    ) : PendingPreferenceUpdate()
    
    /**
     * Update location-specific preferences
     */
    data class LocationPreferencesUpdate(
        override val userId: Int,
        val locationId: Int,
        val notificationsEnabled: Boolean,
        override val timestamp: Long = System.currentTimeMillis()
    ) : PendingPreferenceUpdate()
}
