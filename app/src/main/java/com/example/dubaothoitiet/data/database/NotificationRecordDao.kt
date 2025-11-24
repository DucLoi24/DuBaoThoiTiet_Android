package com.example.dubaothoitiet.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO cho NotificationRecord (lịch sử thông báo)
 */
@Dao
interface NotificationRecordDao {
    
    @Query("SELECT * FROM notification_history WHERE userId = :userId ORDER BY receivedAt DESC")
    suspend fun getAllRecords(userId: Int): List<NotificationRecordEntity>
    
    @Query("SELECT * FROM notification_history WHERE userId = :userId ORDER BY receivedAt DESC")
    fun observeAllRecords(userId: Int): Flow<List<NotificationRecordEntity>>
    
    @Query("SELECT * FROM notification_history WHERE id = :id")
    suspend fun getRecordById(id: Long): NotificationRecordEntity?
    
    @Query("SELECT * FROM notification_history WHERE userId = :userId AND notificationType = :type ORDER BY receivedAt DESC")
    suspend fun getRecordsByType(userId: Int, type: String): List<NotificationRecordEntity>
    
    @Query("SELECT * FROM notification_history WHERE userId = :userId AND receivedAt BETWEEN :startTime AND :endTime ORDER BY receivedAt DESC")
    suspend fun getRecordsByTimeRange(userId: Int, startTime: Long, endTime: Long): List<NotificationRecordEntity>
    
    @Query("SELECT * FROM notification_history WHERE userId = :userId AND notificationType = :type AND receivedAt BETWEEN :startTime AND :endTime ORDER BY receivedAt DESC")
    suspend fun getRecordsByTypeAndTimeRange(userId: Int, type: String, startTime: Long, endTime: Long): List<NotificationRecordEntity>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: NotificationRecordEntity): Long
    
    @Update
    suspend fun updateRecord(record: NotificationRecordEntity)
    
    @Query("UPDATE notification_history SET read = :read WHERE id = :id")
    suspend fun markAsRead(id: Long, read: Boolean = true)
    
    @Query("DELETE FROM notification_history WHERE userId = :userId")
    suspend fun deleteAllRecords(userId: Int)
    
    @Query("DELETE FROM notification_history WHERE receivedAt < :timestamp")
    suspend fun deleteOldRecords(timestamp: Long)
    
    @Query("SELECT COUNT(*) FROM notification_history WHERE userId = :userId AND read = 0")
    suspend fun getUnreadCount(userId: Int): Int
    
    @Query("SELECT COUNT(*) FROM notification_history WHERE userId = :userId AND read = 0")
    fun observeUnreadCount(userId: Int): Flow<Int>
}
