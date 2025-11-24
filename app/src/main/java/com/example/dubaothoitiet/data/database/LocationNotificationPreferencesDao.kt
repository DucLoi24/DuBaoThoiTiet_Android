package com.example.dubaothoitiet.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * DAO cho LocationNotificationPreferences
 */
@Dao
interface LocationNotificationPreferencesDao {
    
    @Query("SELECT * FROM location_notification_preferences WHERE userId = :userId")
    suspend fun getLocationPreferences(userId: Int): List<LocationNotificationPreferencesEntity>
    
    @Query("SELECT * FROM location_notification_preferences WHERE userId = :userId AND locationId = :locationId")
    suspend fun getLocationPreference(userId: Int, locationId: Int): LocationNotificationPreferencesEntity?
    
    @Query("SELECT * FROM location_notification_preferences WHERE userId = :userId")
    fun observeLocationPreferences(userId: Int): Flow<List<LocationNotificationPreferencesEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLocationPreference(preference: LocationNotificationPreferencesEntity)
    
    @Update
    suspend fun updateLocationPreference(preference: LocationNotificationPreferencesEntity)
    
    @Query("DELETE FROM location_notification_preferences WHERE userId = :userId AND locationId = :locationId")
    suspend fun deleteLocationPreference(userId: Int, locationId: Int)
    
    @Query("DELETE FROM location_notification_preferences WHERE userId = :userId")
    suspend fun deleteAllLocationPreferences(userId: Int)
    
    @Query("UPDATE location_notification_preferences SET lastSyncedAt = :timestamp WHERE userId = :userId AND locationId = :locationId")
    suspend fun updateLastSyncedAt(userId: Int, locationId: Int, timestamp: Long)
}
