package com.example.dubaothoitiet.data

/**
 * Mức độ ưu tiên của thông báo
 */
enum class NotificationPriority {
    HIGH,
    MEDIUM,
    LOW;

    companion object {
        fun fromString(value: String): NotificationPriority {
            return when (value.lowercase()) {
                "high" -> HIGH
                "medium" -> MEDIUM
                "low" -> LOW
                else -> MEDIUM
            }
        }
    }

    fun toApiString(): String {
        return name.lowercase()
    }
}

/**
 * Lịch trình nhận thông báo
 */
enum class NotificationSchedule {
    DAYTIME_ONLY,  // Chỉ nhận thông báo từ 6:00-22:00
    FULL_24_7;     // Nhận thông báo 24/7

    companion object {
        fun fromString(value: String): NotificationSchedule {
            return when (value) {
                "daytime_only" -> DAYTIME_ONLY
                "24_7" -> FULL_24_7
                else -> FULL_24_7
            }
        }
    }

    fun toApiString(): String {
        return when (this) {
            DAYTIME_ONLY -> "daytime_only"
            FULL_24_7 -> "24_7"
        }
    }

    fun getDisplayName(): String {
        return when (this) {
            DAYTIME_ONLY -> "Chỉ ban ngày (6:00-22:00)"
            FULL_24_7 -> "24/7"
        }
    }
}

/**
 * Loại sự kiện thời tiết - 17 loại cảnh báo
 */
enum class WeatherEventType(val displayName: String, val apiValue: String, val category: String) {
    // Lũ lụt
    FLOOD_RISK("Nguy cơ lũ lụt", "flood_risk", "water"),
    
    // Mưa
    HEAVY_RAIN("Mưa lớn", "heavy_rain", "water"),
    MODERATE_RAIN("Mưa vừa", "moderate_rain", "water"),
    
    // Bão và gió
    SUPER_TYPHOON("Siêu bão", "super_typhoon", "storm"),
    TYPHOON("Bão mạnh", "typhoon", "storm"),
    TROPICAL_STORM("Bão nhiệt đới", "tropical_storm", "storm"),
    TROPICAL_DEPRESSION("Áp thấp nhiệt đới", "tropical_depression", "storm"),
    STRONG_WIND("Gió mạnh", "strong_wind", "storm"),
    
    // Nhiệt độ
    EXTREME_HEAT("Nắng nóng cực đoan", "extreme_heat", "temperature"),
    EXTREME_COLD("Rét hại", "extreme_cold", "temperature"),
    
    // UV Index
    UV_EXTREME("UV cực kỳ nguy hiểm", "uv_extreme", "health"),
    UV_VERY_HIGH("UV rất cao", "uv_very_high", "health"),
    UV_HIGH("UV cao", "uv_high", "health"),
    
    // Chất lượng không khí
    AQI_HAZARDOUS("AQI nguy hại", "aqi_hazardous", "health"),
    AQI_VERY_UNHEALTHY("AQI rất không tốt", "aqi_very_unhealthy", "health"),
    AQI_UNHEALTHY("AQI không tốt", "aqi_unhealthy", "health"),
    AQI_UNHEALTHY_SENSITIVE("AQI không tốt cho nhóm nhạy cảm", "aqi_unhealthy_sensitive", "health"),
    
    // Cảnh báo chính thức
    OFFICIAL_ALERT("Cảnh báo chính thức", "official_alert", "official"),
    
    // Thời tiết đẹp
    SUNNY("Nắng đẹp", "sunny", "general");

    companion object {
        fun fromApiValue(value: String): WeatherEventType? {
            return values().find { it.apiValue == value }
        }

        fun getAllApiValues(): List<String> {
            return values().map { it.apiValue }
        }
        
        fun getByCategory(category: String): List<WeatherEventType> {
            return values().filter { it.category == category }
        }
        
        fun getAllCategories(): List<String> {
            return values().map { it.category }.distinct()
        }
    }
}

/**
 * Loại thông báo
 */
enum class NotificationType(val displayName: String, val apiValue: String) {
    ALERT("Cảnh báo", "alert"),
    MORNING_SUMMARY("Tóm tắt buổi sáng", "morning_summary"),
    TOMORROW_FORECAST("Dự báo ngày mai", "tomorrow_forecast"),
    WEEKLY_SUMMARY("Tóm tắt tuần", "weekly_summary");

    companion object {
        fun fromApiValue(value: String): NotificationType? {
            return values().find { it.apiValue == value }
        }
    }
}
