package com.example.dubaothoitiet.data

import com.google.gson.annotations.SerializedName

data class WeatherResponse(
    val location: Location,
    val current: Current,
    @SerializedName("forecast") val forecast: ForecastData,
    val alerts: AlertsData? = null
)

data class Location(
    val name: String,
    val region: String,
    val country: String,
    val localtime: String,
    val lat: Double,
    val lon: Double
)

data class Current(
    @SerializedName("temp_c") val tempC: Double,
    @SerializedName("feelslike_c") val feelslikeC: Double, // Đã thêm
    @SerializedName("vis_km") val visKm: Double,             // Đã thêm
    val condition: Condition,
    @SerializedName("wind_kph") val windKph: Double,
    val humidity: Int,
    val uv: Double
)

data class Condition(
    val text: String,
    val icon: String
)

data class ForecastData(
    @SerializedName("forecastday") val forecastDay: List<ForecastDay>
)

data class ForecastDay(
    val date: String,
    val day: Day,
    val astro: Astro,
    val hour: List<Hour>
)

data class Day(
    @SerializedName("maxtemp_c") val maxTempC: Double,
    @SerializedName("mintemp_c") val minTempC: Double,
    @SerializedName("avgtemp_c") val avgTempC: Double,
    @SerializedName("maxwind_kph") val maxWindKph: Double,
    @SerializedName("avghumidity") val avgHumidity: Double,
    @SerializedName("daily_chance_of_rain") val dailyChanceOfRain: Int,
    val uv: Double,
    val condition: Condition
)

data class Astro(
    val sunrise: String,
    val sunset: String
)

data class Hour(
    @SerializedName("time_epoch") val timeEpoch: Long,
    val time: String,
    @SerializedName("temp_c") val tempC: Double,
    val condition: Condition,
    @SerializedName("wind_kph") val windKph: Double,
    val humidity: Int,
    @SerializedName("feelslike_c") val feelslikeC: Double,
    val uv: Double,
    @SerializedName("chance_of_rain") val chanceOfRain: Int,
    @SerializedName("precip_mm") val precipMm: Float? = null,
    @SerializedName("air_quality") val airQuality: AirQuality? = null
)

data class AirQuality(
    @SerializedName("co") val co: Float? = null,
    @SerializedName("no2") val no2: Float? = null,
    @SerializedName("o3") val o3: Float? = null,
    @SerializedName("so2") val so2: Float? = null,
    @SerializedName("pm2_5") val pm25: Float? = null,
    @SerializedName("pm10") val pm10: Float? = null,
    @SerializedName("us-epa-index") val usEpaIndex: Int? = null,
    @SerializedName("gb-defra-index") val gbDefraIndex: Int? = null
)

data class AlertsData(
    val alert: List<WeatherAlert>? = null
)

data class WeatherAlert(
    val headline: String,
    val msgtype: String? = null,
    val severity: String? = null,
    val urgency: String? = null,
    val areas: String? = null,
    val category: String? = null,
    val certainty: String? = null,
    val event: String,
    val note: String? = null,
    val effective: String? = null,
    val expires: String? = null,
    val desc: String,
    val instruction: String? = null
)
