package com.example.dubaothoitiet.service

import android.content.Context
import android.util.Log
import com.example.dubaothoitiet.api.WeatherApiService
import com.example.dubaothoitiet.data.DeviceTokenRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Helper class để quản lý việc đăng ký FCM token với backend
 * 
 * Validates: Requirements 12.1, 12.5
 */
object FirebaseTokenManager {

    private const val TAG = "FirebaseTokenManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Đăng ký FCM token với backend server
     * 
     * @param context Application context
     * @param userId ID của user đã đăng nhập
     * 
     * Validates: Requirements 12.1, 12.5
     */
    fun registerTokenWithServer(context: Context, userId: Int) {
        scope.launch {
            try {
                // Lấy FCM token từ SharedPreferences
                val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                val token = prefs.getString("fcm_token", null)

                if (token.isNullOrEmpty()) {
                    Log.w(TAG, "No FCM token available to register")
                    return@launch
                }

                // Lấy base URL từ SharedPreferences
                val baseUrl = prefs.getString("base_url", "http://10.0.2.2:8000/") 
                    ?: "http://10.0.2.2:8000/"

                // Tạo Retrofit instance
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()

                val apiService = retrofit.create(WeatherApiService::class.java)

                // Tạo request
                val request = DeviceTokenRequest(
                    userId = userId,
                    token = token
                )

                Log.d(TAG, "Registering token with backend for user: $userId")

                // Gửi request lên backend
                val response = apiService.registerDeviceToken(request)

                if (response.isSuccessful) {
                    Log.d(TAG, "Token registered successfully with backend")
                    
                    // Lưu timestamp của lần đăng ký cuối
                    prefs.edit()
                        .putLong("fcm_token_registered_at", System.currentTimeMillis())
                        .apply()
                } else {
                    Log.e(TAG, "Failed to register token: ${response.code()} - ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error registering token with backend", e)
            }
        }
    }

    /**
     * Kiểm tra xem token đã được đăng ký với backend chưa
     * 
     * @param context Application context
     * @return true nếu token đã được đăng ký trong vòng 24 giờ qua
     */
    fun isTokenRegistered(context: Context): Boolean {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val registeredAt = prefs.getLong("fcm_token_registered_at", 0)
        
        if (registeredAt == 0L) {
            return false
        }

        // Kiểm tra xem token có được đăng ký trong vòng 24 giờ qua không
        val twentyFourHoursInMillis = 24 * 60 * 60 * 1000
        val now = System.currentTimeMillis()
        
        return (now - registeredAt) < twentyFourHoursInMillis
    }

    /**
     * Xóa thông tin token đã đăng ký (dùng khi user logout)
     * 
     * @param context Application context
     */
    fun clearTokenRegistration(context: Context) {
        val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit()
            .remove("fcm_token")
            .remove("fcm_token_registered_at")
            .apply()
        
        Log.d(TAG, "Token registration cleared")
    }
}
