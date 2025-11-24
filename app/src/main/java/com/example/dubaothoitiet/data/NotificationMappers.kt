package com.example.dubaothoitiet.data

import com.example.dubaothoitiet.data.database.LocationNotificationPreferencesEntity
import com.example.dubaothoitiet.data.database.NotificationPreferencesEntity
import com.example.dubaothoitiet.data.database.NotificationRecordEntity

/**
 * Extension functions để chuyển đổi giữa các models
 * Giúp code gọn gàng và dễ maintain hơn
 */

// ============= NotificationPreferences Mappers =============

/**
 * Chuyển từ API DTO sang Domain Model
 */
fun NotificationPreferencesResponse.toDomainModel(): NotificationPreferences {
    return NotificationPreferences.fromResponse(this)
}

/**
 * Chuyển từ Entity sang Domain Model
 */
fun NotificationPreferencesEntity.toDomainModel(): NotificationPreferences {
    return NotificationPreferences.fromEntity(this)
}

/**
 * Chuyển từ Domain Model sang API Request
 * Note: toEntity() đã có sẵn trong NotificationPreferences class
 */
fun NotificationPreferences.toUpdateRequest(): UpdatePreferencesRequest {
    return UpdatePreferencesRequest(
        enabledEventTypes = enabledEventTypes.map { it.apiValue },
        notificationSchedule = notificationSchedule.toApiString(),
        morningSummaryEnabled = morningSummaryEnabled,
        tomorrowForecastEnabled = tomorrowForecastEnabled,
        weeklySummaryEnabled = weeklySummaryEnabled,
        timezone = timezone
    )
}

// ============= LocationNotificationPreferences Mappers =============

/**
 * Chuyển từ API Response sang Domain Model
 */
fun LocationPreferenceResponse.toDomainModel(): LocationNotificationPreferences {
    return LocationNotificationPreferences.fromResponse(this)
}

/**
 * Chuyển từ Entity sang Domain Model
 */
fun LocationNotificationPreferencesEntity.toDomainModel(): LocationNotificationPreferences {
    return LocationNotificationPreferences.fromEntity(this)
}

/**
 * Chuyển từ Domain Model sang API Request
 * Note: toEntity() đã có sẵn trong LocationNotificationPreferences class
 */
fun LocationNotificationPreferences.toUpdateRequest(): UpdateLocationPreferenceRequest {
    return UpdateLocationPreferenceRequest(
        notificationsEnabled = notificationsEnabled
    )
}

// ============= NotificationRecord Mappers =============

/**
 * Chuyển từ API Response sang Domain Model
 */
fun NotificationHistoryResponse.toDomainModel(userId: Int): NotificationRecord {
    return NotificationRecord.fromResponse(this, userId)
}

/**
 * Chuyển từ Entity sang Domain Model
 */
fun NotificationRecordEntity.toDomainModel(): NotificationRecord {
    return NotificationRecord.fromEntity(this)
}

/**
 * Chuyển từ Domain Model sang DTO để hiển thị
 * Note: toEntity() đã có sẵn trong NotificationRecord class
 */
fun NotificationRecord.toHistoryDTO(locationName: String? = null): NotificationHistoryDTO {
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

// ============= List Mappers =============

/**
 * Chuyển list entities sang list domain models
 */
fun List<NotificationPreferencesEntity>.toDomainModels(): List<NotificationPreferences> {
    return map { it.toDomainModel() }
}

fun List<LocationNotificationPreferencesEntity>.toLocationDomainModels(): List<LocationNotificationPreferences> {
    return map { it.toDomainModel() }
}

fun List<NotificationRecordEntity>.toRecordDomainModels(): List<NotificationRecord> {
    return map { it.toDomainModel() }
}

fun List<NotificationHistoryResponse>.toRecordDomainModels(userId: Int): List<NotificationRecord> {
    return map { it.toDomainModel(userId) }
}
