package com.example.dubaothoitiet.data

data class TrackLocationRequest(
    val userId: Int,
    val name: String,
    val nameEn: String,
    val lat: Double,
    val lon: Double
)
