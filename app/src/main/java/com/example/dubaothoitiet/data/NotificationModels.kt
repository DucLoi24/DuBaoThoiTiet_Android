package com.example.dubaothoitiet.data

/**
 * Domain model cho notification preferences - sử dụng trong app
 */
data class NotificationPreferences(
    val userId: Int,
    val notificationsEnabled: Boolean = true,
    val enabledEventTypes: List<WeatherEventType>,
    val notificationSchedule: NotificationSchedule,
    val morningSummaryEnabled: Boolean,
    val tomorrowForecastEnabled: Boolean,
    val weeklySummaryEnabled: Boolean,
    val timezone: String,
    val lastSyncedAt: Long = 0
) {
    /**
     * Chuyển đổi sang DTO để gửi lên API
     * TODO: Implement NotificationPreferencesDTO
     */
    // fun toDTO(): NotificationPreferencesDTO {
    //     return NotificationPreferencesDTO(
    //         notificationsEnabled = notificationsEnabled,
    //         enabledEventTypes = enabledEventTypes.map { it.apiValue },
    //         notificationSchedule = notificationSchedule.toApiString(),
    //         morningSummaryEnabled = morningSummaryEnabled,
    //         tomorrowForecastEnabled = tomorrowForecastEnabled,
    //         weeklySummaryEnabled = weeklySummaryEnabled,
    //         timezone = timezone
    //     )
    // }

    /**
     * Chuyển đổi sang Entity để lưu vào Room database
     */
    fun toEntity(): com.example.dubaothoitiet.data.database.NotificationPreferencesEntity {
        return com.example.dubaothoitiet.data.database.NotificationPreferencesEntity(
            userId = userId,
            notificationsEnabled = notificationsEnabled,
            enabledEventTypes = enabledEventTypes.map { it.apiValue },
            notificationSchedule = notificationSchedule.toApiString(),
            morningSummaryEnabled = morningSummaryEnabled,
            tomorrowForecastEnabled = tomorrowForecastEnabled,
            weeklySummaryEnabled = weeklySummaryEnabled,
            timezone = timezone,
            lastSyncedAt = lastSyncedAt
        )
    }

    companion object {
        /**
         * Tạo preferences mặc định
         */
        fun default(userId: Int): NotificationPreferences {
            return NotificationPreferences(
                userId = userId,
                notificationsEnabled = true,
                enabledEventTypes = WeatherEventType.values().toList(),
                notificationSchedule = NotificationSchedule.FULL_24_7,
                morningSummaryEnabled = true,
                tomorrowForecastEnabled = true,
                weeklySummaryEnabled = false,
                timezone = "Asia/Ho_Chi_Minh"
            )
        }

        /**
         * Chuyển đổi từ Entity sang domain model
         */
        fun fromEntity(entity: com.example.dubaothoitiet.data.database.NotificationPreferencesEntity): NotificationPreferences {
            return NotificationPreferences(
                userId = entity.userId,
                notificationsEnabled = entity.notificationsEnabled,
                enabledEventTypes = entity.enabledEventTypes.mapNotNull { 
                    WeatherEventType.fromApiValue(it) 
                },
                notificationSchedule = NotificationSchedule.fromString(entity.notificationSchedule),
                morningSummaryEnabled = entity.morningSummaryEnabled,
                tomorrowForecastEnabled = entity.tomorrowForecastEnabled,
                weeklySummaryEnabled = entity.weeklySummaryEnabled,
                timezone = entity.timezone,
                lastSyncedAt = entity.lastSyncedAt
            )
        }

        /**
         * Chuyển đổi từ API response sang domain model
         */
        fun fromResponse(response: NotificationPreferencesResponse): NotificationPreferences {
            val prefs = response.preferences
            return NotificationPreferences(
                userId = prefs.userId,
                notificationsEnabled = prefs.notificationsEnabled,
                enabledEventTypes = prefs.enabledEventTypes.mapNotNull { 
                    WeatherEventType.fromApiValue(it) 
                },
                notificationSchedule = NotificationSchedule.fromString(prefs.notificationSchedule),
                morningSummaryEnabled = prefs.morningSummaryEnabled,
                tomorrowForecastEnabled = prefs.tomorrowForecastEnabled,
                weeklySummaryEnabled = prefs.weeklySummaryEnabled,
                timezone = prefs.timezone,
                lastSyncedAt = System.currentTimeMillis()
            )
        }
    }
}

/**
 * Domain model cho location-specific preferences
 */
data class LocationNotificationPreferences(
    val userId: Int,
    val locationId: Int,
    val notificationsEnabled: Boolean,
    val lastSyncedAt: Long = 0
) {
    /**
     * Chuyển đổi sang DTO để gửi lên API
     */
    fun toDTO(): LocationPreferenceDTO {
        return LocationPreferenceDTO(
            locationId = locationId,
            notificationsEnabled = notificationsEnabled
        )
    }

    /**
     * Chuyển đổi sang Entity để lưu vào Room database
     */
    fun toEntity(): com.example.dubaothoitiet.data.database.LocationNotificationPreferencesEntity {
        return com.example.dubaothoitiet.data.database.LocationNotificationPreferencesEntity(
            userId = userId,
            locationId = locationId,
            notificationsEnabled = notificationsEnabled,
            lastSyncedAt = lastSyncedAt
        )
    }

    companion object {
        /**
         * Chuyển đổi từ Entity sang domain model
         */
        fun fromEntity(entity: com.example.dubaothoitiet.data.database.LocationNotificationPreferencesEntity): LocationNotificationPreferences {
            return LocationNotificationPreferences(
                userId = entity.userId,
                locationId = entity.locationId,
                notificationsEnabled = entity.notificationsEnabled,
                lastSyncedAt = entity.lastSyncedAt
            )
        }

        /**
         * Chuyển đổi từ API response sang domain model
         */
        fun fromResponse(response: LocationPreferenceResponse): LocationNotificationPreferences {
            return LocationNotificationPreferences(
                userId = response.userId,
                locationId = response.locationId,
                notificationsEnabled = response.notificationsEnabled,
                lastSyncedAt = System.currentTimeMillis()
            )
        }
    }
}

/**
 * Domain model cho notification record
 */
data class NotificationRecord(
    val id: Long = 0,
    val userId: Int,
    val locationId: Int?,
    val notificationType: NotificationType,
    val title: String,
    val body: String,
    val priority: NotificationPriority,
    val receivedAt: Long,
    val data: Map<String, String> = emptyMap(),
    val read: Boolean = false
) {
    /**
     * Chuyển đổi sang Entity để lưu vào Room database
     */
    fun toEntity(): com.example.dubaothoitiet.data.database.NotificationRecordEntity {
        return com.example.dubaothoitiet.data.database.NotificationRecordEntity(
            id = id,
            userId = userId,
            locationId = locationId,
            notificationType = notificationType.apiValue,
            title = title,
            body = body,
            priority = priority.toApiString(),
            receivedAt = receivedAt,
            data = data,
            read = read
        )
    }

    /**
     * Chuyển đổi sang DTO để hiển thị trong UI
     */
    fun toDTO(locationName: String?): NotificationHistoryDTO {
        return NotificationHistoryDTO(
            id = id,
            notificationType = notificationType.apiValue,
            title = title,
            body = body,
            priority = priority.toApiString(),
            receivedAt = receivedAt,
            locationName = locationName,
            read = read
        )
    }

    companion object {
        /**
         * Chuyển đổi từ Entity sang domain model
         */
        fun fromEntity(entity: com.example.dubaothoitiet.data.database.NotificationRecordEntity): NotificationRecord {
            return NotificationRecord(
                id = entity.id,
                userId = entity.userId,
                locationId = entity.locationId,
                notificationType = NotificationType.fromApiValue(entity.notificationType) 
                    ?: NotificationType.ALERT,
                title = entity.title,
                body = entity.body,
                priority = NotificationPriority.fromString(entity.priority),
                receivedAt = entity.receivedAt,
                data = entity.data,
                read = entity.read
            )
        }

        /**
         * Chuyển đổi từ API response sang domain model
         */
        fun fromResponse(response: NotificationHistoryResponse, userId: Int): NotificationRecord {
            return NotificationRecord(
                id = response.recordId,
                userId = userId,
                locationId = response.locationId,
                notificationType = NotificationType.fromApiValue(response.notificationType) 
                    ?: NotificationType.ALERT,
                title = response.title,
                body = response.body,
                priority = NotificationPriority.fromString(response.priority),
                receivedAt = parseTimestamp(response.sentAt),
                data = emptyMap(),
                read = false
            )
        }

        private fun parseTimestamp(@Suppress("UNUSED_PARAMETER") timestamp: String): Long {
            // Parse ISO 8601 timestamp từ API
            // Tạm thời return current time, sẽ implement parser đầy đủ sau
            return System.currentTimeMillis()
        }
    }
}

/**
 * Sync status cho preferences
 */
enum class SyncStatus {
    IDLE,
    SYNCING,
    SUCCESS,
    ERROR
}
