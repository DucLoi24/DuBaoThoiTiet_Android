package com.example.dubaothoitiet.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity lưu trữ lịch sử thông báo đã nhận
 */
@Entity(tableName = "notification_history")
data class NotificationRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Int,
    val locationId: Int?,
    val notificationType: String,
    val title: String,
    val body: String,
    val priority: String,
    val receivedAt: Long,
    val data: Map<String, String> = emptyMap(),
    val read: Boolean = false
)
