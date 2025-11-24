package com.example.dubaothoitiet.api

import com.example.dubaothoitiet.data.AuthRequest
import com.example.dubaothoitiet.data.LoginResponse
import com.example.dubaothoitiet.data.RegisteredUserResponse
import com.example.dubaothoitiet.data.TrackLocationRequest
import com.example.dubaothoitiet.data.WeatherResponse
import com.example.dubaothoitiet.data.ExtremeAlert
import com.example.dubaothoitiet.data.AdviceResponse
import com.example.dubaothoitiet.data.CheckAdviceResponse
import com.example.dubaothoitiet.data.TrackedLocation
import com.example.dubaothoitiet.data.DeviceTokenRequest
import com.example.dubaothoitiet.data.NotificationPreferencesResponse
import com.example.dubaothoitiet.data.UpdatePreferencesRequest
import com.example.dubaothoitiet.data.LocationPreferenceResponse
import com.example.dubaothoitiet.data.UpdateLocationPreferenceRequest
import com.example.dubaothoitiet.data.NotificationHistoryListResponse
import com.example.dubaothoitiet.data.DeviceTokenRegistrationRequest
import com.example.dubaothoitiet.data.DeviceTokenRegistrationResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

interface WeatherApiService {
    @GET("api/weather")
    suspend fun getWeather(
        @Query("q") city: String,
        @Query("days") days: Int = 3,
        @Query("aqi") aqi: String = "yes",
        @Query("alerts") alerts: String = "yes"
    ): WeatherResponse

    @POST("api/register/")
    suspend fun register(@Body request: AuthRequest): Response<RegisteredUserResponse>

    @POST("api/login/")
    suspend fun login(@Body request: AuthRequest): Response<LoginResponse>

    @POST("api/locations/track/")
    suspend fun trackLocation(@Body request: TrackLocationRequest): Response<Unit>

    @GET("api/alerts/")
    suspend fun getAlerts(
        @Query("q") locationNameEn: String // Truyền tên địa điểm không dấu
    ): Response<List<ExtremeAlert>>

    @GET("api/advice/")
    suspend fun getAdvice(
        @Query("q") locationNameEn: String
    ): Response<AdviceResponse>

    @GET("api/check-advice/")
    suspend fun checkAdvice(
        @Query("q") locationNameEn: String
    ): Response<CheckAdviceResponse>

    @GET("api/locations/tracked/")
    suspend fun getTrackedLocations(
        @Query("user_id") userId: Int
    ): Response<List<TrackedLocation>>

    @POST("api/locations/delete/")
    suspend fun deleteTrackedLocation(
        @Query("user_id") userId: Int,
        @Query("location_id") locationId: Int
    ): Response<Unit>

    @POST("api/device-token/register/")
    suspend fun registerDeviceToken(
        @Body request: DeviceTokenRequest
    ): Response<Unit>

    // ==================== Notification Preferences Endpoints ====================
    
    /**
     * Lấy notification preferences của user hiện tại
     */
    @GET("api/notifications/preferences/")
    suspend fun getNotificationPreferences(
        @Query("user_id") userId: Int
    ): Response<NotificationPreferencesResponse>

    /**
     * Cập nhật notification preferences của user hiện tại
     */
    @POST("api/notifications/preferences/")
    suspend fun updateNotificationPreferences(
        @Query("user_id") userId: Int,
        @Body request: UpdatePreferencesRequest
    ): Response<NotificationPreferencesResponse>

    // ==================== Location Preferences Endpoints ====================
    
    /**
     * Lấy notification preferences cho một location cụ thể
     */
    @GET("api/notifications/preferences/location/{id}/")
    suspend fun getLocationPreferences(
        @Path("id") locationId: Int,
        @Query("user_id") userId: Int
    ): Response<LocationPreferenceResponse>

    /**
     * Cập nhật notification preferences cho một location cụ thể
     */
    @POST("api/notifications/preferences/location/{id}/")
    suspend fun updateLocationPreferences(
        @Path("id") locationId: Int,
        @Query("user_id") userId: Int,
        @Body request: UpdateLocationPreferenceRequest
    ): Response<LocationPreferenceResponse>

    // ==================== Notification History Endpoints ====================
    
    /**
     * Lấy lịch sử thông báo với filtering
     * @param notificationType Lọc theo loại thông báo (alert, morning_summary, etc.)
     * @param startDate Lọc từ ngày (ISO 8601 format)
     * @param endDate Lọc đến ngày (ISO 8601 format)
     * @param page Số trang (pagination)
     * @param pageSize Số items mỗi trang
     */
    @GET("api/notifications/history/")
    suspend fun getNotificationHistory(
        @Query("notification_type") notificationType: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("page") page: Int? = null,
        @Query("page_size") pageSize: Int? = null
    ): Response<NotificationHistoryListResponse>

    // ==================== Device Token Management ====================
    
    /**
     * Đăng ký device token cho push notifications
     */
    @POST("api/notifications/device-token/")
    suspend fun registerNotificationDeviceToken(
        @Body request: DeviceTokenRegistrationRequest
    ): Response<DeviceTokenRegistrationResponse>
}

object RetrofitInstance {
    // Chạy lệnh này để có thể truy cập server chính xác:
    // adb reverse tcp:8000 tcp:8000
    private const val BASE_URL = "http://127.0.0.1:8000/"

    /**
     * Logging interceptor để log các request/response
     * Chỉ log BODY trong debug mode để tránh log quá nhiều trong production
     */
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /**
     * OkHttpClient với logging interceptor và timeout configuration
     */
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS) // Timeout kết nối
        .readTimeout(300, TimeUnit.SECONDS)    // Timeout đọc dữ liệu (TĂNG LÊN 2 PHÚT)
        .writeTimeout(300, TimeUnit.SECONDS)   // Timeout ghi dữ liệu
        .build()

    val api: WeatherApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }
}
