package com.example.dubaothoitiet.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Entity lưu trữ preferences thông báo của user
 */
@Entity(tableName = "notification_preferences")
data class NotificationPreferencesEntity(
    @PrimaryKey val userId: Int,
    val notificationsEnabled: Boolean = true,
    val enabledEventTypes: List<String>,
    val notificationSchedule: String,  // "24_7" or "daytime_only"
    val morningSummaryEnabled: Boolean = true,
    val tomorrowForecastEnabled: Boolean = true,
    val weeklySummaryEnabled: Boolean = false,
    val timezone: String = "Asia/Ho_Chi_Minh",
    val lastSyncedAt: Long = 0
)

/**
 * Type converters cho Room database
 */
class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toStringList(value: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
    
    @TypeConverter
    fun fromStringMap(value: Map<String, String>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toStringMap(value: String): Map<String, String> {
        val mapType = object : TypeToken<Map<String, String>>() {}.type
        return gson.fromJson(value, mapType)
    }
}
