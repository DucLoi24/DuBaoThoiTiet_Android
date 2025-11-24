package com.example.dubaothoitiet.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO cho NotificationPreferences
 */
@Dao
interface NotificationPreferencesDao {
    
    @Query("SELECT * FROM notification_preferences WHERE userId = :userId")
    suspend fun getPreferences(userId: Int): NotificationPreferencesEntity?
    
    @Query("SELECT * FROM notification_preferences WHERE userId = :userId")
    fun observePreferences(userId: Int): Flow<NotificationPreferencesEntity?>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreferences(preferences: NotificationPreferencesEntity)
    
    @Update
    suspend fun updatePreferences(preferences: NotificationPreferencesEntity)
    
    @Query("DELETE FROM notification_preferences WHERE userId = :userId")
    suspend fun deletePreferences(userId: Int)
    
    @Query("UPDATE notification_preferences SET lastSyncedAt = :timestamp WHERE userId = :userId")
    suspend fun updateLastSyncedAt(userId: Int, timestamp: Long)
}
