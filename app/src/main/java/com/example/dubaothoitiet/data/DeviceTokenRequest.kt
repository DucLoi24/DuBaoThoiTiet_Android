package com.example.dubaothoitiet.data

import com.google.gson.annotations.SerializedName

data class DeviceTokenRequest(
    @SerializedName("user_id") val userId: Int,
    val token: String
)
