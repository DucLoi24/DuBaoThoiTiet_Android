package com.example.dubaothoitiet.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.dubaothoitiet.data.NotificationPriority

/**
 * Quản lý các notification channels cho Android 8.0+
 * 
 * Class này chịu trách nhiệm:
 * - Tạo và cấu hình các notification channels
 * - Gán channel phù hợp dựa trên mức độ ưu tiên
 * - Cấu hình sound, vibration, importance cho mỗi channel
 * 
 * Validates: Requirements 18.1, 18.4
 */
class NotificationChannelManager(private val context: Context) {

    companion object {
        // Channel IDs
        const val CHANNEL_HIGH_PRIORITY = "weather_alerts_high"
        const val CHANNEL_SCHEDULED = "weather_scheduled"
        const val CHANNEL_GENERAL = "weather_general"

        // Channel Names
        private const val CHANNEL_HIGH_PRIORITY_NAME = "Cảnh báo thời tiết khẩn cấp"
        private const val CHANNEL_SCHEDULED_NAME = "Tóm tắt thời tiết định kỳ"
        private const val CHANNEL_GENERAL_NAME = "Thông báo chung"

        // Channel Descriptions
        private const val CHANNEL_HIGH_PRIORITY_DESC = "Cảnh báo về thời tiết nguy hiểm như mưa lớn, bão, nhiệt độ cực đoan"
        private const val CHANNEL_SCHEDULED_DESC = "Tóm tắt buổi sáng, dự báo ngày mai và tóm tắt tuần"
        private const val CHANNEL_GENERAL_DESC = "Các thông báo thời tiết khác"
    }

    /**
     * Tạo tất cả notification channels khi app khởi động
     * 
     * Chỉ hoạt động trên Android 8.0 (API 26) trở lên
     * Mỗi channel được cấu hình với:
     * - Importance level phù hợp
     * - Sound và vibration settings
     * - Description cho người dùng
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun createNotificationChannels() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. Channel cho cảnh báo ưu tiên cao
        val highPriorityChannel = NotificationChannel(
            CHANNEL_HIGH_PRIORITY,
            CHANNEL_HIGH_PRIORITY_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_HIGH_PRIORITY_DESC
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 250, 500) // Pattern: wait, vibrate, wait, vibrate
            
            // Sử dụng default notification sound
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            setSound(defaultSoundUri, AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
            
            enableLights(true)
            setShowBadge(true)
            
            // Cho phép bypass Do Not Disturb cho cảnh báo khẩn cấp
            setBypassDnd(true)
        }

        // 2. Channel cho thông báo định kỳ (tóm tắt, dự báo)
        val scheduledChannel = NotificationChannel(
            CHANNEL_SCHEDULED,
            CHANNEL_SCHEDULED_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_SCHEDULED_DESC
            enableVibration(false) // Không rung cho thông báo định kỳ
            
            // Sử dụng default notification sound với volume thấp hơn
            val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            setSound(defaultSoundUri, AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build())
            
            enableLights(false)
            setShowBadge(true)
        }

        // 3. Channel cho thông báo chung
        val generalChannel = NotificationChannel(
            CHANNEL_GENERAL,
            CHANNEL_GENERAL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = CHANNEL_GENERAL_DESC
            enableVibration(false)
            setSound(null, null) // Không có âm thanh cho thông báo chung
            enableLights(false)
            setShowBadge(true)
        }

        // Tạo tất cả channels
        notificationManager.createNotificationChannel(highPriorityChannel)
        notificationManager.createNotificationChannel(scheduledChannel)
        notificationManager.createNotificationChannel(generalChannel)
    }

    /**
     * Lấy channel ID phù hợp dựa trên mức độ ưu tiên
     * 
     * Method này thực hiện logic gán notification channel theo priority level,
     * đảm bảo mỗi loại thông báo được hiển thị với cấu hình phù hợp về
     * sound, vibration, và importance level.
     * 
     * @param priority Mức độ ưu tiên của thông báo
     * @return Channel ID tương ứng
     * 
     * Channel Assignment Logic:
     * - HIGH priority -> CHANNEL_HIGH_PRIORITY
     *   + Dùng cho: Cảnh báo thời tiết nguy hiểm (mưa lớn, bão, nhiệt độ cực đoan)
     *   + Settings: High importance, sound enabled, vibration enabled, bypass DND
     * 
     * - MEDIUM priority -> CHANNEL_SCHEDULED
     *   + Dùng cho: Thông báo định kỳ (tóm tắt buổi sáng, dự báo ngày mai, tóm tắt tuần)
     *   + Settings: Default importance, sound enabled, no vibration
     * 
     * - LOW priority -> CHANNEL_GENERAL
     *   + Dùng cho: Thông báo chung, thông tin không khẩn cấp
     *   + Settings: Low importance, no sound, no vibration
     * 
     * Validates: Requirements 18.2, 18.5
     */
    fun getChannelForPriority(priority: NotificationPriority): String {
        return when (priority) {
            NotificationPriority.HIGH -> CHANNEL_HIGH_PRIORITY
            NotificationPriority.MEDIUM -> CHANNEL_SCHEDULED
            NotificationPriority.LOW -> CHANNEL_GENERAL
        }
    }

    /**
     * Kiểm tra xem notification channels đã được tạo chưa
     * 
     * @return true nếu tất cả channels đã tồn tại, false nếu chưa
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun areChannelsCreated(): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        return notificationManager.getNotificationChannel(CHANNEL_HIGH_PRIORITY) != null &&
               notificationManager.getNotificationChannel(CHANNEL_SCHEDULED) != null &&
               notificationManager.getNotificationChannel(CHANNEL_GENERAL) != null
    }

    /**
     * Verify channel assignment cho notification type và priority
     * Helper method để đảm bảo logic gán channel đúng
     * 
     * @param notificationType Loại thông báo
     * @param priority Mức độ ưu tiên
     * @return Channel ID được gán
     */
    fun verifyChannelAssignment(notificationType: String, priority: NotificationPriority): String {
        val channelId = getChannelForPriority(priority)
        
        // Verify logic:
        // - Alert notifications với HIGH priority -> CHANNEL_HIGH_PRIORITY
        // - Scheduled notifications (morning_summary, tomorrow_forecast, weekly_summary) -> CHANNEL_SCHEDULED
        // - General notifications -> CHANNEL_GENERAL
        
        when (notificationType) {
            "alert" -> {
                // Alerts thường có HIGH priority
                if (priority == NotificationPriority.HIGH && channelId != CHANNEL_HIGH_PRIORITY) {
                    throw IllegalStateException("High priority alert must use CHANNEL_HIGH_PRIORITY")
                }
            }
            "morning_summary", "tomorrow_forecast", "weekly_summary" -> {
                // Scheduled notifications thường có MEDIUM priority
                if (priority == NotificationPriority.MEDIUM && channelId != CHANNEL_SCHEDULED) {
                    throw IllegalStateException("Scheduled notification must use CHANNEL_SCHEDULED")
                }
            }
        }
        
        return channelId
    }

    /**
     * Xóa tất cả notification channels (dùng cho testing hoặc reset)
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun deleteAllChannels() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        notificationManager.deleteNotificationChannel(CHANNEL_HIGH_PRIORITY)
        notificationManager.deleteNotificationChannel(CHANNEL_SCHEDULED)
        notificationManager.deleteNotificationChannel(CHANNEL_GENERAL)
    }
}
