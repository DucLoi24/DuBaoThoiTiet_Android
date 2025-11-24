package com.example.dubaothoitiet

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import com.example.dubaothoitiet.service.NotificationChannelManager
import com.example.dubaothoitiet.data.NotificationRepository
import com.example.dubaothoitiet.data.NetworkMonitor
import com.example.dubaothoitiet.data.database.NotificationDatabase
import com.example.dubaothoitiet.api.RetrofitInstance
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Application class cho Weather App
 * 
 * Chức năng:
 * - Khởi tạo Firebase Cloud Messaging
 * - Tạo notification channels (Android 8.0+)
 * - Lấy và đăng ký FCM token với backend
 * - Request notification permission (Android 13+)
 * - Sync notification preferences từ backend khi app khởi động
 * 
 * Validates: Requirements 12.1, 15.2, 18.1
 */
class WeatherApplication : Application() {

    companion object {
        private const val TAG = "WeatherApplication"
    }
    
    // Application-level coroutine scope
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        
        Log.d(TAG, "WeatherApplication onCreate")
        
        // 1. Khởi tạo Firebase
        initializeFirebase()
        
        // 2. Tạo notification channels (Android 8.0+)
        createNotificationChannels()
        
        // 3. Lấy FCM token và đăng ký với backend
        registerFCMToken()
        
        // 4. Sync notification preferences từ backend
        syncNotificationPreferences()
    }

    /**
     * Khởi tạo Firebase Cloud Messaging
     * 
     * Validates: Requirements 12.1
     */
    private fun initializeFirebase() {
        try {
            FirebaseApp.initializeApp(this)
            Log.d(TAG, "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase", e)
        }
    }

    /**
     * Tạo notification channels lần đầu khởi chạy
     * Chỉ hoạt động trên Android 8.0 (API 26) trở lên
     * 
     * Validates: Requirements 18.1
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channelManager = NotificationChannelManager(this)
                
                // Kiểm tra xem channels đã được tạo chưa
                if (!channelManager.areChannelsCreated()) {
                    channelManager.createNotificationChannels()
                    Log.d(TAG, "Notification channels created successfully")
                } else {
                    Log.d(TAG, "Notification channels already exist")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error creating notification channels", e)
            }
        } else {
            Log.d(TAG, "Notification channels not needed for Android < 8.0")
        }
    }

    /**
     * Lấy FCM token và đăng ký với backend
     * 
     * Token sẽ được lấy bất đồng bộ và gửi lên backend khi có user đăng nhập
     * Nếu chưa có user, token sẽ được lưu vào SharedPreferences và gửi sau khi đăng nhập
     * 
     * Validates: Requirements 12.1
     */
    private fun registerFCMToken() {
        try {
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                    return@addOnCompleteListener
                }

                // Lấy FCM token
                val token = task.result
                Log.d(TAG, "FCM Token: $token")

                // Lưu token vào SharedPreferences
                getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("fcm_token", token)
                    .apply()

                // Kiểm tra xem user đã đăng nhập chưa
                val userId = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                    .getInt("user_id", -1)

                if (userId != -1) {
                    // Nếu đã đăng nhập, gửi token lên backend ngay
                    Log.d(TAG, "User logged in, sending token to backend")
                    com.example.dubaothoitiet.service.FirebaseTokenManager.registerTokenWithServer(
                        this@WeatherApplication,
                        userId
                    )
                } else {
                    // Nếu chưa đăng nhập, token sẽ được gửi sau khi user đăng nhập
                    Log.d(TAG, "User not logged in, token will be sent after login")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error registering FCM token", e)
        }
    }
    
    /**
     * Sync notification preferences từ backend khi app khởi động
     * 
     * Fetch preferences mới nhất từ backend và cập nhật Room database local
     * Xử lý sync errors một cách graceful - không block app startup nếu sync thất bại
     * 
     * Validates: Requirements 15.2
     */
    private fun syncNotificationPreferences() {
        applicationScope.launch {
            try {
                // Kiểm tra xem user đã đăng nhập chưa
                val prefs = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val userId = prefs.getInt("user_id", -1)
                
                if (userId == -1) {
                    Log.d(TAG, "User not logged in, skipping preference sync")
                    return@launch
                }
                
                Log.d(TAG, "Starting preference sync for user $userId")
                
                // Khởi tạo repository
                val database = NotificationDatabase.getDatabase(this@WeatherApplication)
                val apiService = RetrofitInstance.api
                val networkMonitor = NetworkMonitor(this@WeatherApplication)
                val repository = NotificationRepository(apiService, database, networkMonitor)
                
                // Sync preferences từ backend
                val result = repository.syncPreferences(userId)
                
                when (result) {
                    is com.example.dubaothoitiet.data.Result.Success -> {
                        Log.d(TAG, "Preference sync completed successfully")
                        
                        // Lưu timestamp của lần sync cuối
                        prefs.edit()
                            .putLong("last_preference_sync", System.currentTimeMillis())
                            .apply()
                    }
                    is com.example.dubaothoitiet.data.Result.Error -> {
                        Log.w(TAG, "Preference sync failed: ${result.message}", result.exception)
                        // Không block app startup, chỉ log error
                        // User vẫn có thể sử dụng preferences đã lưu trong local database
                    }
                    else -> {
                        Log.w(TAG, "Unexpected result type during preference sync")
                    }
                }
                
                // Sync pending updates nếu có (từ lần offline trước)
                val pendingCount = repository.getPendingUpdateCount()
                if (pendingCount > 0) {
                    Log.d(TAG, "Found $pendingCount pending updates, attempting to sync")
                    val syncResult = repository.syncPendingUpdates()
                    
                    when (syncResult) {
                        is com.example.dubaothoitiet.data.Result.Success -> {
                            Log.d(TAG, "All pending updates synced successfully")
                        }
                        is com.example.dubaothoitiet.data.Result.Error -> {
                            Log.w(TAG, "Some pending updates failed to sync: ${syncResult.message}")
                        }
                        else -> {
                            Log.w(TAG, "Unexpected result type during pending updates sync")
                        }
                    }
                }
                
            } catch (e: Exception) {
                // Xử lý lỗi một cách graceful - không crash app
                Log.e(TAG, "Error during preference sync", e)
                // App vẫn tiếp tục hoạt động với preferences local
            }
        }
    }
}
