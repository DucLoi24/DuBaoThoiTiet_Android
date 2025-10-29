package com.example.dubaothoitiet.data

import com.google.gson.annotations.SerializedName

// Data class này cần xử lý 2 trường hợp: có advice hoặc chỉ có status
data class CheckAdviceResponse(
    val type: String?, // "advice", "warning", hoặc null nếu stale
    @SerializedName("message_vi") val messageVi: String?, // Nội dung advice/warning hoặc null nếu stale
    val status: String?, // "stale" hoặc null nếu có advice
    @SerializedName("generated_time") val generatedTime: String? // Thời gian tạo (có thể null)
) {
    // Hàm tiện ích để kiểm tra xem có advice hợp lệ không
    fun hasValidAdvice(): Boolean = type != null && messageVi != null && status == null
}