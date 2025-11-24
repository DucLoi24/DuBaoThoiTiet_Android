package com.example.dubaothoitiet.data.database

import androidx.room.Entity

/**
 * Entity lưu trữ preferences thông báo cho từng location cụ thể
 */
@Entity(
    tableName = "location_notification_preferences",
    primaryKeys = ["userId", "locationId"]
)
data class LocationNotificationPreferencesEntity(
    val userId: Int,
    val locationId: Int,
    val notificationsEnabled: Boolean = true,
    val lastSyncedAt: Long = 0
)
