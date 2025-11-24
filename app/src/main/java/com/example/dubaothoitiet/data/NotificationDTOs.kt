package com.example.dubaothoitiet.data

import com.google.gson.annotations.SerializedName

/**
 * DTO cho notification preferences - gửi/nhận từ API
 */
data class NotificationPreferencesDTO(
    @SerializedName("enabled_event_types")
    val enabledEventTypes: List<String>,
    
    @SerializedName("notification_schedule")
    val notificationSchedule: String,
    
    @SerializedName("morning_summary_enabled")
    val morningSummaryEnabled: Boolean,
    
    @SerializedName("tomorrow_forecast_enabled")
    val tomorrowForecastEnabled: Boolean,
    
    @SerializedName("weekly_summary_enabled")
    val weeklySummaryEnabled: Boolean,
    
    @SerializedName("timezone")
    val timezone: String
)

/**
 * DTO cho location-specific preferences
 */
data class LocationPreferenceDTO(
    @SerializedName("location_id")
    val locationId: Int,
    
    @SerializedName("notifications_enabled")
    val notificationsEnabled: Boolean
)

/**
 * Response từ API khi lấy preferences (inner object)
 */
data class NotificationPreferencesData(
    @SerializedName("preference_id")
    val preferenceId: Long?,
    
    @SerializedName("user")
    val userId: Int,  // Backend trả về "user" không phải "user_id"
    
    @SerializedName("notifications_enabled")
    val notificationsEnabled: Boolean = true,
    
    @SerializedName("enabled_event_types")
    val enabledEventTypes: List<String>,
    
    @SerializedName("notification_schedule")
    val notificationSchedule: String,
    
    @SerializedName("morning_summary_enabled")
    val morningSummaryEnabled: Boolean,
    
    @SerializedName("tomorrow_forecast_enabled")
    val tomorrowForecastEnabled: Boolean,
    
    @SerializedName("weekly_summary_enabled")
    val weeklySummaryEnabled: Boolean,
    
    @SerializedName("timezone")
    val timezone: String,
    
    @SerializedName("created_at")
    val createdAt: String?,
    
    @SerializedName("updated_at")
    val updatedAt: String?
)

/**
 * Response wrapper từ API
 */
data class NotificationPreferencesResponse(
    @SerializedName("message")
    val message: String?,
    
    @SerializedName("preferences")
    val preferences: NotificationPreferencesData
)

/**
 * Request để cập nhật preferences
 */
data class UpdatePreferencesRequest(
    @SerializedName("notifications_enabled")
    val notificationsEnabled: Boolean = true,
    
    @SerializedName("enabled_event_types")
    val enabledEventTypes: List<String>,
    
    @SerializedName("notification_schedule")
    val notificationSchedule: String,
    
    @SerializedName("morning_summary_enabled")
    val morningSummaryEnabled: Boolean,
    
    @SerializedName("tomorrow_forecast_enabled")
    val tomorrowForecastEnabled: Boolean,
    
    @SerializedName("weekly_summary_enabled")
    val weeklySummaryEnabled: Boolean,
    
    @SerializedName("timezone")
    val timezone: String
)

/**
 * Response từ API khi lấy location preferences
 */
data class LocationPreferenceResponse(
    @SerializedName("id")
    val id: Long?,
    
    @SerializedName("user_id")
    val userId: Int,
    
    @SerializedName("location_id")
    val locationId: Int,
    
    @SerializedName("notifications_enabled")
    val notificationsEnabled: Boolean,
    
    @SerializedName("created_at")
    val createdAt: String?,
    
    @SerializedName("updated_at")
    val updatedAt: String?
)

/**
 * Request để cập nhật location preferences
 */
data class UpdateLocationPreferenceRequest(
    @SerializedName("notifications_enabled")
    val notificationsEnabled: Boolean
)

/**
 * DTO cho notification history item - hiển thị trong UI
 */
data class NotificationHistoryDTO(
    val id: Long,
    val notificationType: String,
    val title: String,
    val body: String,
    val priority: String,
    val receivedAt: Long,
    val locationName: String?,
    val read: Boolean
)

/**
 * Response từ API khi lấy notification history
 */
data class NotificationHistoryResponse(
    @SerializedName("record_id")
    val recordId: Long,
    
    @SerializedName("user_id")
    val userId: Int,
    
    @SerializedName("location_id")
    val locationId: Int?,
    
    @SerializedName("notification_type")
    val notificationType: String,
    
    @SerializedName("title")
    val title: String,
    
    @SerializedName("body")
    val body: String,
    
    @SerializedName("priority")
    val priority: String,
    
    @SerializedName("sent_at")
    val sentAt: String,
    
    @SerializedName("delivered")
    val delivered: Boolean,
    
    @SerializedName("fcm_message_id")
    val fcmMessageId: String?
)

/**
 * Response danh sách notification history
 */
data class NotificationHistoryListResponse(
    @SerializedName("count")
    val count: Int,
    
    @SerializedName("next")
    val next: String?,
    
    @SerializedName("previous")
    val previous: String?,
    
    @SerializedName("results")
    val results: List<NotificationHistoryResponse>
)

/**
 * Request để đăng ký device token
 */
data class DeviceTokenRegistrationRequest(
    @SerializedName("device_token")
    val deviceToken: String,
    
    @SerializedName("device_type")
    val deviceType: String = "android"
)

/**
 * Response khi đăng ký device token thành công
 */
data class DeviceTokenRegistrationResponse(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("token")
    val token: String
)

/**
 * Error response từ API
 */
data class ApiErrorResponse(
    @SerializedName("error")
    val error: String?,
    
    @SerializedName("detail")
    val detail: String?,
    
    @SerializedName("message")
    val message: String?
) {
    fun getErrorMessage(): String {
        return error ?: detail ?: message ?: "Đã xảy ra lỗi không xác định"
    }
}
