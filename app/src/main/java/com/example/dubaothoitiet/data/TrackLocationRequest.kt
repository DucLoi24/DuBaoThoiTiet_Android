package com.example.dubaothoitiet.data

import com.google.gson.annotations.SerializedName

data class TrackLocationRequest(
    @SerializedName("user_id") val userId: Int,
    val name: String,
    @SerializedName("name_en") val nameEn: String,
    @SerializedName("latitude") val lat: Double,
    @SerializedName("longitude") val lon: Double
)
