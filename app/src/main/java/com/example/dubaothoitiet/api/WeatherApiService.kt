package com.example.dubaothoitiet.api

import com.example.dubaothoitiet.data.AuthRequest
import com.example.dubaothoitiet.data.LoginResponse
import com.example.dubaothoitiet.data.RegisteredUserResponse
import com.example.dubaothoitiet.data.TrackLocationRequest
import com.example.dubaothoitiet.data.WeatherResponse
import com.example.dubaothoitiet.data.ExtremeAlert
import com.example.dubaothoitiet.data.AdviceResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

interface WeatherApiService {
    @GET("api/weather")
    suspend fun getWeather(
        @Query("q") city: String,
        @Query("days") days: Int = 7
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
}

object RetrofitInstance {
    // Chạy lệnh này để có thể truy cập server chính xác:
    // adb reverse tcp:8000 tcp:8000
    private const val BASE_URL = "http://127.0.0.1:8000/"

    private val okHttpClient = OkHttpClient.Builder()
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
