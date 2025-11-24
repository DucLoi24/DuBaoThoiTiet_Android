package com.example.dubaothoitiet.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.dubaothoitiet.MainActivity
import com.example.dubaothoitiet.R
import com.example.dubaothoitiet.api.RetrofitInstance
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class WeatherNotificationService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var updateJob: Job? = null

    companion object {
        private const val TAG = "WeatherNotifService"
        private const val CHANNEL_ID = "weather_widget_channel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_REFRESH = "ACTION_REFRESH"

        fun start(context: Context) {
            val intent = Intent(context, WeatherNotificationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, WeatherNotificationService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createInitialNotification())
        startWeatherUpdates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
            }
            ACTION_REFRESH -> {
                refreshWeather()
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        updateJob?.cancel()
        serviceScope.cancel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Thông tin thời tiết",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Hiển thị thông tin thời tiết hiện tại"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createInitialNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Đang tải thời tiết...")
            .setContentText("Vui lòng đợi")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun startWeatherUpdates() {
        updateJob = serviceScope.launch {
            // Delay 2 giây trước lần update đầu tiên để không block UI
            delay(2000)
            
            while (isActive) {
                try {
                    updateWeatherNotification()
                    delay(1 * 60 * 1000) // Cập nhật mỗi 1 phút
                } catch (e: Exception) {
                    Log.e(TAG, "Error updating weather", e)
                    delay(60 * 1000) // Retry sau 1 phút nếu lỗi
                }
            }
        }
    }

    private fun refreshWeather() {
        serviceScope.launch {
            updateWeatherNotification()
        }
    }

    private suspend fun updateWeatherNotification() {
        try {
            // Lấy vị trí từ SharedPreferences hoặc dùng vị trí mặc định
            val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
            val lastCity = prefs.getString("last_city", "Hanoi") ?: "Hanoi"
            
            // Gọi API lấy thời tiết
            val weather = RetrofitInstance.api.getWeather(lastCity, days = 1)
            
            val notification = createWeatherNotification(weather)
                
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.notify(NOTIFICATION_ID, notification)
            
            Log.d(TAG, "Weather notification updated for $lastCity")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update weather notification", e)
        }
    }

    private fun createWeatherNotification(weather: com.example.dubaothoitiet.data.WeatherResponse): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent để refresh
        val refreshIntent = Intent(this, WeatherNotificationService::class.java).apply {
            action = ACTION_REFRESH
        }
        val refreshPendingIntent = PendingIntent.getService(
            this, 1, refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent để stop
        val stopIntent = Intent(this, WeatherNotificationService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val current = weather.current
        val location = weather.location
        
        // Tìm thay đổi trạng thái thời tiết tiếp theo (không phải giờ tiếp theo)
        val nextWeatherChange = findNextWeatherChange(weather, current.condition.text)

        // Tạo thông tin dự báo ngắn gọn cho subText
        val forecastSubText = if (nextWeatherChange != null) {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val hourTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(nextWeatherChange.time)
            val timeStr = if (hourTime != null) timeFormat.format(hourTime) else "sắp tới"
            "$timeStr: ${nextWeatherChange.condition.text} ${nextWeatherChange.tempC.toInt()}°C"
        } else {
            "Không có thay đổi"
        }

        // Tạo expanded layout với nhiều thông tin hơn
        val expandedText = buildWeatherDetails(current, nextWeatherChange, weather)
        val expandedView = NotificationCompat.BigTextStyle()
            .bigText(expandedText)
            .setBigContentTitle("${location.name} - ${current.tempC.toInt()}°C")

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${location.name} - ${current.tempC.toInt()}°C")
            .setContentText("${current.condition.text} • ${current.humidity}% • ${current.windKph.toInt()}km/h")
            .setSubText("Tiếp theo $forecastSubText")
            .setStyle(expandedView)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                android.R.drawable.ic_menu_rotate,
                "Làm mới",
                refreshPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Tắt",
                stopPendingIntent
            )
            .build()
    }

    private fun findNextWeatherChange(
        weather: com.example.dubaothoitiet.data.WeatherResponse,
        currentCondition: String
    ): com.example.dubaothoitiet.data.Hour? {
        val now = Date()
        val allHours = weather.forecast.forecastDay.firstOrNull()?.hour ?: return null
        
        // Tìm giờ đầu tiên có trạng thái thời tiết khác với hiện tại
        return allHours.find { hour ->
            val hourTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(hour.time)
            hourTime?.after(now) == true && hour.condition.text != currentCondition
        }
    }

    private fun buildWeatherDetails(
        current: com.example.dubaothoitiet.data.Current,
        nextChange: com.example.dubaothoitiet.data.Hour?,
        weather: com.example.dubaothoitiet.data.WeatherResponse
    ): String {
        val sb = StringBuilder()
        
        // Thông tin hiện tại - rút gọn
        sb.append("🌡️ ${current.tempC.toInt()}°C (cảm giác ${current.feelslikeC.toInt()}°C)\n")
        sb.append("☁️ ${current.condition.text}\n")
        sb.append("💧 Độ ẩm ${current.humidity}% • 💨 Gió ${current.windKph.toInt()} km/h\n")
        
        // Lấy số giờ dự báo từ SharedPreferences
        val prefs = getSharedPreferences("weather_widget_prefs", Context.MODE_PRIVATE)
        val forecastHours = prefs.getInt("forecast_hours", 3)
        
        // Dự báo nhiệt độ trong N giờ tới - hiển thị trên 1 dòng
        val upcomingTemps = getUpcomingTemperatures(weather, forecastHours)
        if (upcomingTemps.isNotEmpty()) {
            sb.append("\n🕐 Nhiệt độ ${forecastHours}h tới: ")
            sb.append(upcomingTemps.joinToString(" • ") { "${it.first}:${it.second}°" })
            sb.append("\n")
        }
        
        // Dự báo thay đổi thời tiết tiếp theo - rút gọn
        if (nextChange != null) {
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val hourTime = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(nextChange.time)
            val timeStr = if (hourTime != null) timeFormat.format(hourTime) else "sắp tới"
            
            sb.append("\n📅 $timeStr: ${nextChange.condition.text} ${nextChange.tempC.toInt()}°C")
            if (nextChange.chanceOfRain > 0) {
                sb.append(" • 🌧️ ${nextChange.chanceOfRain}%")
            }
        }
        
        return sb.toString()
    }
    
    private fun getUpcomingTemperatures(
        weather: com.example.dubaothoitiet.data.WeatherResponse,
        hours: Int
    ): List<Pair<String, Int>> {
        val now = Date()
        val allHours = weather.forecast.forecastDay.flatMap { it.hour }
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val parseFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        
        return allHours
            .mapNotNull { hour ->
                val hourTime = parseFormat.parse(hour.time)
                if (hourTime != null && hourTime.after(now)) {
                    val timeDiff = (hourTime.time - now.time) / (1000 * 60 * 60) // Chênh lệch giờ
                    if (timeDiff <= hours) {
                        Pair(timeFormat.format(hourTime), hour.tempC.toInt())
                    } else null
                } else null
            }
            .take(hours)
    }
}
