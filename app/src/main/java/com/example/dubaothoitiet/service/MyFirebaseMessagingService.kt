package com.example.dubaothoitiet.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.dubaothoitiet.MainActivity
import com.example.dubaothoitiet.R
import com.example.dubaothoitiet.api.WeatherApiService
import com.example.dubaothoitiet.data.NotificationPriority
import com.example.dubaothoitiet.data.NotificationRecord
import com.example.dubaothoitiet.data.NotificationType
import com.example.dubaothoitiet.data.database.NotificationDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Firebase Cloud Messaging Service cho Weather App
 * 
 * Chức năng:
 * - Nhận và xử lý push notifications từ backend
 * - Phân loại và hiển thị thông báo theo priority
 * - Lưu lịch sử thông báo vào local database
 * - Nhóm thông báo khi có nhiều thông báo cùng lúc
 * - Ghi đè DND cho cảnh báo ưu tiên cao
 * - Cập nhật device token lên backend khi thay đổi
 * 
 * Validates: Requirements 12.1, 12.2, 12.3, 12.5, 17.1, 17.2, 17.3, 17.4
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var notificationChannelManager: NotificationChannelManager
    private lateinit var database: NotificationDatabase
    
    // Group notifications by type
    private val notificationGroups = mutableMapOf<String, MutableList<Int>>()
    
    companion object {
        private const val TAG = "WeatherFCMService"
        
        // Notification group keys
        private const val GROUP_KEY_ALERTS = "weather_alerts_group"
        private const val GROUP_KEY_SCHEDULED = "weather_scheduled_group"
        
        // Summary notification IDs
        private const val SUMMARY_ID_ALERTS = 1000
        private const val SUMMARY_ID_SCHEDULED = 2000
    }

    override fun onCreate() {
        super.onCreate()
        notificationChannelManager = NotificationChannelManager(this)
        database = NotificationDatabase.getDatabase(this)
        
        // Tạo notification channels nếu chưa có
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!notificationChannelManager.areChannelsCreated()) {
                notificationChannelManager.createNotificationChannels()
                Log.d(TAG, "Notification channels created")
            }
        }
    }

    /**
     * Xử lý khi nhận được FCM token mới
     * Lưu token vào SharedPreferences và gửi lên backend
     * 
     * Validates: Requirements 12.1, 12.5
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token received: $token")
        
        // Lưu token vào SharedPreferences
        getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("fcm_token", token)
            .apply()
        
        // Gửi token lên backend
        sendTokenToBackend(token)
    }

    /**
     * Xử lý khi nhận được notification từ FCM
     * Phân loại notification và xử lý tương ứng
     * 
     * Validates: Requirements 12.2, 17.1, 17.2
     */
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        
        Log.d(TAG, "Message received from: ${message.from}")
        Log.d(TAG, "Message data: ${message.data}")
        
        // Lấy data từ message
        val data = message.data
        
        if (data.isEmpty()) {
            Log.w(TAG, "Received empty message data")
            return
        }
        
        // Xác định loại notification
        val notificationType = data["notification_type"] ?: "alert"
        
        when (notificationType) {
            "alert" -> handleWeatherAlert(data)
            "morning_summary", "tomorrow_forecast", "weekly_summary" -> handleScheduledNotification(data)
            else -> {
                Log.w(TAG, "Unknown notification type: $notificationType")
                handleWeatherAlert(data) // Default to alert
            }
        }
    }

    /**
     * Xử lý thông báo cảnh báo thời tiết nguy hiểm
     * 
     * Validates: Requirements 12.3, 12.4, 17.4
     */
    private fun handleWeatherAlert(data: Map<String, String>) {
        Log.d(TAG, "Handling weather alert")
        
        val title = data["title"] ?: "Cảnh báo thời tiết"
        val body = data["body"] ?: ""
        val priorityStr = data["priority"] ?: "high"
        val priority = NotificationPriority.fromString(priorityStr)
        val locationId = data["location_id"]?.toIntOrNull()
        val locationName = data["location_name"] ?: ""
        val alertType = data["alert_type"] ?: "unknown"
        
        Log.d(TAG, "Alert details: type=$alertType, location=$locationName, priority=$priorityStr")
        
        // Hiển thị notification với deep link data
        showNotification(
            title = title,
            body = body,
            priority = priority,
            notificationType = NotificationType.ALERT,
            data = data,
            groupKey = GROUP_KEY_ALERTS
        )
        
        // Lưu vào lịch sử
        saveNotificationToHistory(
            title = title,
            body = body,
            priority = priority,
            notificationType = NotificationType.ALERT,
            locationId = locationId,
            data = data
        )
        
        Log.d(TAG, "Weather alert handled: $alertType with priority $priorityStr")
    }

    /**
     * Xử lý thông báo định kỳ (tóm tắt buổi sáng, dự báo ngày mai, tóm tắt tuần)
     * 
     * Validates: Requirements 12.2, 12.4
     */
    private fun handleScheduledNotification(data: Map<String, String>) {
        Log.d(TAG, "Handling scheduled notification")
        
        val title = data["title"] ?: "Thông báo thời tiết"
        val body = data["body"] ?: ""
        val notificationTypeStr = data["notification_type"] ?: "morning_summary"
        val notificationType = NotificationType.fromApiValue(notificationTypeStr) 
            ?: NotificationType.MORNING_SUMMARY
        val locationId = data["location_id"]?.toIntOrNull()
        val locationName = data["location_name"] ?: ""
        
        Log.d(TAG, "Scheduled notification details: type=$notificationTypeStr, location=$locationName")
        
        // Scheduled notifications luôn có priority MEDIUM
        val priority = NotificationPriority.MEDIUM
        
        // Hiển thị notification với deep link data
        showNotification(
            title = title,
            body = body,
            priority = priority,
            notificationType = notificationType,
            data = data,
            groupKey = GROUP_KEY_SCHEDULED
        )
        
        // Lưu vào lịch sử
        saveNotificationToHistory(
            title = title,
            body = body,
            priority = priority,
            notificationType = notificationType,
            locationId = locationId,
            data = data
        )
        
        Log.d(TAG, "Scheduled notification handled: $notificationTypeStr")
    }

    /**
     * Hiển thị notification với cấu hình dựa trên priority
     * Hỗ trợ notification grouping và DND override cho high priority
     * 
     * Method này thực hiện:
     * 1. Gán notification vào channel phù hợp dựa trên priority (Requirements 18.2)
     * 2. Sử dụng channel-specific settings cho sound và visual (Requirements 18.5)
     * 3. Cấu hình notification builder với priority-specific settings
     * 4. Hỗ trợ notification grouping (Requirements 17.3)
     * 5. DND override cho high-priority alerts (Requirements 17.4)
     * 6. Deep linking cho tap notification (Requirements 12.4)
     * 
     * Validates: Requirements 12.3, 12.4, 17.3, 17.4, 18.2, 18.5
     */
    private fun showNotification(
        title: String,
        body: String,
        priority: NotificationPriority,
        notificationType: NotificationType,
        data: Map<String, String>,
        groupKey: String
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Gán notification vào channel phù hợp dựa trên priority
        // Channel assignment đảm bảo notification sử dụng đúng sound, vibration, và importance settings
        val channelId = notificationChannelManager.getChannelForPriority(priority)
        
        Log.d(TAG, "Channel assignment: priority=$priority -> channelId=$channelId")
        
        // Tạo intent để mở app khi click notification với deep link data
        // Validates: Requirements 12.4
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            
            // Thêm flag để MainActivity biết đây là từ notification tap
            putExtra("from_notification", true)
            
            // Thêm notification type
            putExtra("notification_type", notificationType.apiValue)
            
            // Thêm tất cả data từ FCM message để navigate đến màn hình chi tiết
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
            
            // Thêm các thông tin quan trọng cho navigation
            putExtra("notification_title", title)
            putExtra("notification_body", body)
            putExtra("notification_priority", priority.name)
            
            Log.d(TAG, "Deep link data added to intent: type=${notificationType.apiValue}, " +
                    "location_id=${data["location_id"]}, alert_type=${data["alert_type"]}")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build notification với cấu hình dựa trên priority
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setGroup(groupKey) // Nhóm thông báo
        
        // Cấu hình dựa trên priority
        when (priority) {
            NotificationPriority.HIGH -> {
                notificationBuilder
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setCategory(NotificationCompat.CATEGORY_ALARM)
                    .setVibrate(longArrayOf(0, 500, 250, 500))
                
                // Ghi đè DND cho cảnh báo ưu tiên cao (Android 8.0+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Channel đã được cấu hình với setBypassDnd(true)
                    Log.d(TAG, "High priority alert - DND bypass enabled")
                }
            }
            NotificationPriority.MEDIUM -> {
                notificationBuilder
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            }
            NotificationPriority.LOW -> {
                notificationBuilder
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
            }
        }
        
        // Generate unique notification ID
        val notificationId = System.currentTimeMillis().toInt()
        
        // Hiển thị notification
        notificationManager.notify(notificationId, notificationBuilder.build())
        
        // Track notification ID cho grouping
        trackNotificationForGrouping(groupKey, notificationId)
        
        // Hiển thị summary notification nếu có nhiều notifications
        showGroupSummaryIfNeeded(groupKey, notificationManager)
        
        Log.d(TAG, "Notification displayed with ID: $notificationId, priority: $priority, channel: $channelId")
    }

    /**
     * Track notification ID cho grouping
     * 
     * Validates: Requirements 17.3
     */
    private fun trackNotificationForGrouping(groupKey: String, notificationId: Int) {
        synchronized(notificationGroups) {
            val group = notificationGroups.getOrPut(groupKey) { mutableListOf() }
            group.add(notificationId)
            
            // Giữ tối đa 10 notifications trong group
            if (group.size > 10) {
                group.removeAt(0)
            }
        }
    }

    /**
     * Hiển thị summary notification nếu có nhiều hơn 1 notification trong group
     * 
     * Validates: Requirements 17.3
     */
    private fun showGroupSummaryIfNeeded(groupKey: String, notificationManager: NotificationManager) {
        val notificationCount = synchronized(notificationGroups) {
            notificationGroups[groupKey]?.size ?: 0
        }
        
        if (notificationCount <= 1) {
            return // Không cần summary cho 1 notification
        }
        
        // Xác định channel và summary ID dựa trên group
        val (channelId, summaryId, summaryTitle) = when (groupKey) {
            GROUP_KEY_ALERTS -> Triple(
                NotificationChannelManager.CHANNEL_HIGH_PRIORITY,
                SUMMARY_ID_ALERTS,
                "Cảnh báo thời tiết"
            )
            GROUP_KEY_SCHEDULED -> Triple(
                NotificationChannelManager.CHANNEL_SCHEDULED,
                SUMMARY_ID_SCHEDULED,
                "Thông báo thời tiết"
            )
            else -> return
        }
        
        // Tạo summary notification
        val summaryNotification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(summaryTitle)
            .setContentText("Bạn có $notificationCount thông báo mới")
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()
        
        notificationManager.notify(summaryId, summaryNotification)
        Log.d(TAG, "Group summary displayed for $groupKey with $notificationCount notifications")
    }

    /**
     * Lưu notification vào lịch sử trong local database
     * Hoạt động ngay cả khi app không chạy
     * 
     * Validates: Requirements 17.5
     */
    private fun saveNotificationToHistory(
        title: String,
        body: String,
        priority: NotificationPriority,
        notificationType: NotificationType,
        locationId: Int?,
        data: Map<String, String>
    ) {
        serviceScope.launch {
            try {
                // Lấy userId từ SharedPreferences
                val userId = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .getInt("user_id", -1)
                
                if (userId == -1) {
                    Log.w(TAG, "Cannot save notification to history: user not logged in")
                    return@launch
                }
                
                val record = NotificationRecord(
                    userId = userId,
                    locationId = locationId,
                    notificationType = notificationType,
                    title = title,
                    body = body,
                    priority = priority,
                    receivedAt = System.currentTimeMillis(),
                    data = data,
                    read = false
                )
                
                val recordId = database.notificationRecordDao().insertRecord(record.toEntity())
                Log.d(TAG, "Notification saved to history with ID: $recordId")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving notification to history", e)
            }
        }
    }

    /**
     * Public method để lưu notification vào lịch sử
     * Có thể được gọi từ bên ngoài service nếu cần
     * 
     * Validates: Requirements 17.5
     */
    fun saveToHistory(
        title: String,
        body: String,
        priority: NotificationPriority,
        notificationType: NotificationType,
        locationId: Int? = null,
        data: Map<String, String> = emptyMap()
    ) {
        saveNotificationToHistory(
            title = title,
            body = body,
            priority = priority,
            notificationType = notificationType,
            locationId = locationId,
            data = data
        )
    }

    /**
     * Gửi device token lên backend
     * 
     * Validates: Requirements 12.1, 12.5
     */
    private fun sendTokenToBackend(token: String) {
        serviceScope.launch {
            try {
                // Lấy userId từ SharedPreferences
                val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val userId = prefs.getInt("user_id", -1)
                
                if (userId == -1) {
                    Log.w(TAG, "Cannot send token to backend: user not logged in")
                    return@launch
                }
                
                // Tạo API service
                val baseUrl = prefs.getString("base_url", "http://10.0.2.2:8000/") ?: "http://10.0.2.2:8000/"
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                
                val apiService = retrofit.create(WeatherApiService::class.java)
                
                // Gửi token lên backend
                val request = com.example.dubaothoitiet.data.DeviceTokenRequest(
                    userId = userId,
                    token = token
                )
                
                val response = apiService.registerDeviceToken(request)
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Device token successfully sent to backend")
                } else {
                    Log.e(TAG, "Failed to send device token: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending device token to backend", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cleanup
        serviceScope.launch {
            // Clear old notification groups
            synchronized(notificationGroups) {
                notificationGroups.clear()
            }
        }
    }
}
