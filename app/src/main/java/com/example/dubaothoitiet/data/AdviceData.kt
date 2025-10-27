package com.example.dubaothoitiet.data

import com.google.gson.annotations.SerializedName

// Data class khớp với JSON trả về từ /api/advice/
data class AdviceResponse(
    val type: String, // "advice", "warning", hoặc "error"
    @SerializedName("message_vi") val messageVi: String
)