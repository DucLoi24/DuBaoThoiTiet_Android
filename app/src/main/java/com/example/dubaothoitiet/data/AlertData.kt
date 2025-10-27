package com.example.dubaothoitiet.data

import com.google.gson.annotations.SerializedName

// Data class khớp với cấu trúc JSON trả về từ API /api/alerts/
data class ExtremeAlert(
    @SerializedName("event_id") val eventId: Int,
    @SerializedName("analysis_time") val analysisTime: String, // Giữ là String để đơn giản
    val severity: String, // MEDIUM, HIGH, CRITICAL
    @SerializedName("impact_field") val impactField: String, // AGRICULTURE, INFRASTRUCTURE, PUBLIC_HEALTH
    @SerializedName("forecast_details_vi") val forecastDetailsVi: String,
    @SerializedName("actionable_advice_vi") val actionableAdviceVi: String? // Có thể null
)