package com.example.dubaothoitiet.data

import com.google.gson.annotations.SerializedName

data class TrackedLocation(
    val id: Int,
    val name: String,
    @SerializedName("temp_c") val tempC: Double,
    @SerializedName("condition_text") val conditionText: String,
    val icon: String,
    @SerializedName("wind_kph") val windKph: Double,
    @SerializedName("chance_of_rain") val chanceOfRain: Int,
    val humidity: Int
)