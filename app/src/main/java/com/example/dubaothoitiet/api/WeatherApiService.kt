package com.example.dubaothoitiet.api

import com.example.dubaothoitiet.data.AuthRequest
import com.example.dubaothoitiet.data.LoginResponse
import com.example.dubaothoitiet.data.RegisteredUserResponse
import com.example.dubaothoitiet.data.TrackLocationRequest
import com.example.dubaothoitiet.data.WeatherResponse
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

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
}

object RetrofitInstance {
    // Chạy lệnh này để có thể truy cập server chính xác:
    // adb reverse tcp:8000 tcp:8000
    private const val BASE_URL = "http://127.0.0.1:8000/"

    val api: WeatherApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(WeatherApiService::class.java)
    }
}
