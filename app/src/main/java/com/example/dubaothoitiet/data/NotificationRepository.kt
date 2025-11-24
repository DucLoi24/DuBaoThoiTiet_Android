package com.example.dubaothoitiet.data

import android.util.Log
import com.example.dubaothoitiet.api.WeatherApiService
import com.example.dubaothoitiet.data.database.NotificationDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.delay
import java.io.IOException

/**
 * Repository quản lý notification preferences và history
 * Xử lý sync giữa local database (Room) và remote API
 * Hỗ trợ offline mode với queuing và retry logic
 * 
 * Validates: Requirements 11.5, 15.1, 15.2, 15.3, 15.4, 15.5
 */
class NotificationRepository(
    private val apiService: WeatherApiService,
    private val database: NotificationDatabase,
    private val networkMonitor: NetworkMonitor
) {
    
    private val preferencesDao = database.notificationPreferencesDao()
    private val locationPreferencesDao = database.locationNotificationPreferencesDao()
    private val recordDao = database.notificationRecordDao()
    
    // Queue cho các updates đang chờ sync khi offline
    private val pendingUpdates = mutableListOf<PendingPreferenceUpdate>()
    
    companion object {
        private const val TAG = "NotificationRepository"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INITIAL_BACKOFF_MS = 1000L
    }
    
    // ==================== Notification Preferences ====================
    
    /**
     * Lấy preferences từ local database trước, sau đó sync với remote nếu cần
     * Validates: Requirements 15.1, 15.2
     */
    suspend fun getPreferences(userId: Int): Result<NotificationPreferences> {
        return try {
            // Lấy từ local database trước
            val localPrefs = preferencesDao.getPreferences(userId)
            
            if (localPrefs != null) {
                Log.d(TAG, "Loaded preferences from local database for user $userId")
                Result.Success(NotificationPreferences.fromEntity(localPrefs))
            } else {
                // Nếu chưa có local, fetch từ remote
                Log.d(TAG, "No local preferences found, fetching from remote for user $userId")
                fetchPreferencesFromRemote(userId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting preferences for user $userId", e)
            Result.Error(e, "Không thể tải preferences")
        }
    }
    
    /**
     * Observe preferences từ local database với reactive updates
     * Validates: Requirements 15.1
     */
    fun observePreferences(userId: Int): Flow<NotificationPreferences?> {
        return preferencesDao.observePreferences(userId).map { entity ->
            entity?.let { NotificationPreferences.fromEntity(it) }
        }
    }
    
    /**
     * Cập nhật preferences - sync với remote TRƯỚC, sau đó update local
     * Nếu offline, queue update để sync sau
     * Validates: Requirements 11.5, 15.1, 15.3, 15.4
     */
    suspend fun updatePreferences(preferences: NotificationPreferences): Result<Unit> {
        return try {
            // 1. Kiểm tra kết nối mạng
            if (!networkMonitor.isNetworkAvailable()) {
                Log.w(TAG, "No network available, queuing preference update for user ${preferences.userId}")
                queuePreferenceUpdate(
                    PendingPreferenceUpdate.NotificationPreferencesUpdate(
                        userId = preferences.userId,
                        preferences = preferences
                    )
                )
                return Result.Error(IOException("No network available"), "Không có kết nối mạng")
            }
            
            // 2. Sync với remote API TRƯỚC
            val syncResult = syncPreferencesToRemote(preferences)
            
            when (syncResult) {
                is Result.Success<*> -> {
                    // 3. CHỈ update local database KHI remote thành công
                    val entity = preferences.copy(lastSyncedAt = System.currentTimeMillis()).toEntity()
                    preferencesDao.insertPreferences(entity)
                    Log.d(TAG, "Successfully synced preferences to remote and updated local for user ${preferences.userId}")
                    Result.Success(Unit)
                }
                is Result.Error -> {
                    // Sync failed, queue để retry sau
                    Log.w(TAG, "Remote sync failed: ${syncResult.message}, queuing for retry for user ${preferences.userId}")
                    queuePreferenceUpdate(
                        PendingPreferenceUpdate.NotificationPreferencesUpdate(
                            userId = preferences.userId,
                            preferences = preferences
                        )
                    )
                    // Trả về Error để ViewModel biết sync thất bại
                    Result.Error(syncResult.exception ?: Exception("Sync failed"), syncResult.message ?: "Không thể đồng bộ")
                }
                else -> Result.Error(Exception("Unknown result"), "Lỗi không xác định")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating preferences for user ${preferences.userId}", e)
            Result.Error(e, "Không thể cập nhật preferences")
        }
    }
    
    /**
     * Sync preferences từ remote về local (dùng khi app startup)
     * Validates: Requirements 15.2, 15.3
     */
    suspend fun syncPreferences(userId: Int): Result<Unit> {
        return try {
            Log.d(TAG, "Starting preference sync for user $userId")
            
            val result = fetchPreferencesFromRemote(userId)
            
            if (result is Result.Success) {
                Log.d(TAG, "Preference sync completed successfully for user $userId")
                Result.Success(Unit)
            } else {
                Log.w(TAG, "Preference sync failed for user $userId")
                result as Result.Error
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing preferences for user $userId", e)
            Result.Error(e, "Không thể đồng bộ preferences")
        }
    }

    /**
     * Fetch preferences từ remote API và lưu vào local database
     * Sử dụng last-write-wins strategy cho conflict resolution
     * Validates: Requirements 15.2, 15.3
     */
    private suspend fun fetchPreferencesFromRemote(userId: Int): Result<NotificationPreferences> {
        return withRetry(MAX_RETRY_ATTEMPTS) {
            try {
                val response = apiService.getNotificationPreferences(userId)
                
                if (response.isSuccessful && response.body() != null) {
                    val remotePrefs = NotificationPreferences.fromResponse(response.body()!!)
                    
                    // Lấy local preferences để so sánh timestamp
                    val localPrefs = preferencesDao.getPreferences(userId)
                    
                    // Last-write-wins strategy
                    val shouldUpdate = localPrefs == null || 
                                      remotePrefs.lastSyncedAt >= localPrefs.lastSyncedAt
                    
                    if (shouldUpdate) {
                        preferencesDao.insertPreferences(remotePrefs.toEntity())
                        Log.d(TAG, "Updated local preferences from remote for user $userId")
                    } else {
                        Log.d(TAG, "Local preferences are newer, skipping update for user $userId")
                    }
                    
                    Result.Success(remotePrefs)
                } else {
                    val errorMsg = "API error: ${response.code()}"
                    Log.e(TAG, errorMsg)
                    Result.Error(IOException(errorMsg), "Không thể tải preferences từ server")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error fetching preferences", e)
                throw e
            }
        }
    }
    
    /**
     * Sync preferences lên remote API
     * Validates: Requirements 15.1, 15.4
     */
    private suspend fun syncPreferencesToRemote(preferences: NotificationPreferences): Result<Unit> {
        return withRetry(MAX_RETRY_ATTEMPTS) {
            try {
                val request = UpdatePreferencesRequest(
                    notificationsEnabled = preferences.notificationsEnabled,
                    enabledEventTypes = preferences.enabledEventTypes.map { it.apiValue },
                    notificationSchedule = preferences.notificationSchedule.toApiString(),
                    morningSummaryEnabled = preferences.morningSummaryEnabled,
                    tomorrowForecastEnabled = preferences.tomorrowForecastEnabled,
                    weeklySummaryEnabled = preferences.weeklySummaryEnabled,
                    timezone = preferences.timezone
                )
                
                Log.d(TAG, "Syncing preferences to remote - notificationsEnabled: ${request.notificationsEnabled}")
                Log.d(TAG, "Request details: $request")
                
                val response = apiService.updateNotificationPreferences(preferences.userId, request)
                
                if (response.isSuccessful) {
                    Log.d(TAG, "Successfully synced preferences to remote - Response: ${response.body()}")
                    Result.Success(Unit)
                } else {
                    val errorMsg = "API error: ${response.code()} - ${response.errorBody()?.string()}"
                    Log.e(TAG, errorMsg)
                    Result.Error(IOException(errorMsg), "Không thể đồng bộ preferences")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error syncing preferences", e)
                throw e
            }
        }
    }
    
    // ==================== Location Preferences ====================
    
    /**
     * Lấy location preferences cho một location cụ thể
     * Validates: Requirements 15.1
     */
    suspend fun getLocationPreferences(userId: Int, locationId: Int): Result<LocationNotificationPreferences> {
        return try {
            val localPref = locationPreferencesDao.getLocationPreference(userId, locationId)
            
            if (localPref != null) {
                Result.Success(LocationNotificationPreferences.fromEntity(localPref))
            } else {
                // Fetch từ remote nếu chưa có local
                fetchLocationPreferencesFromRemote(userId, locationId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting location preferences", e)
            Result.Error(e, "Không thể tải location preferences")
        }
    }
    
    /**
     * Observe tất cả location preferences của user
     * Validates: Requirements 15.1
     */
    fun observeLocationPreferences(userId: Int): Flow<List<LocationNotificationPreferences>> {
        return locationPreferencesDao.observeLocationPreferences(userId).map { entities ->
            entities.map { LocationNotificationPreferences.fromEntity(it) }
        }
    }
    
    /**
     * Cập nhật location preferences
     * Nếu offline, queue update để sync sau
     * Validates: Requirements 11.5, 15.1, 15.3, 15.4
     */
    suspend fun updateLocationPreferences(
        userId: Int,
        locationId: Int,
        notificationsEnabled: Boolean
    ): Result<Unit> {
        return try {
            // 1. Update local
            val preference = LocationNotificationPreferences(
                userId = userId,
                locationId = locationId,
                notificationsEnabled = notificationsEnabled,
                lastSyncedAt = System.currentTimeMillis()
            )
            locationPreferencesDao.insertLocationPreference(preference.toEntity())
            Log.d(TAG, "Updated local location preferences for user $userId, location $locationId")
            
            // 2. Kiểm tra kết nối mạng
            if (!networkMonitor.isNetworkAvailable()) {
                Log.w(TAG, "No network available, queuing location preference update")
                queuePreferenceUpdate(
                    PendingPreferenceUpdate.LocationPreferencesUpdate(
                        userId = userId,
                        locationId = locationId,
                        notificationsEnabled = notificationsEnabled
                    )
                )
                return Result.Success(Unit)
            }
            
            // 3. Sync với remote nếu có mạng
            val syncResult = syncLocationPreferencesToRemote(userId, locationId, notificationsEnabled)
            
            when (syncResult) {
                is Result.Success<*> -> {
                    Log.d(TAG, "Successfully synced location preferences to remote")
                    Result.Success(Unit)
                }
                is Result.Error -> {
                    Log.w(TAG, "Remote sync failed, queuing for retry")
                    queuePreferenceUpdate(
                        PendingPreferenceUpdate.LocationPreferencesUpdate(
                            userId = userId,
                            locationId = locationId,
                            notificationsEnabled = notificationsEnabled
                        )
                    )
                    Result.Success(Unit)
                }
                else -> Result.Success(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating location preferences", e)
            Result.Error(e, "Không thể cập nhật location preferences")
        }
    }
    
    /**
     * Fetch location preferences từ remote
     */
    private suspend fun fetchLocationPreferencesFromRemote(
        @Suppress("UNUSED_PARAMETER") userId: Int,
        locationId: Int
    ): Result<LocationNotificationPreferences> {
        return withRetry(MAX_RETRY_ATTEMPTS) {
            try {
                val response = apiService.getLocationPreferences(locationId, userId)
                
                if (response.isSuccessful && response.body() != null) {
                    val remotePref = LocationNotificationPreferences.fromResponse(response.body()!!)
                    locationPreferencesDao.insertLocationPreference(remotePref.toEntity())
                    Result.Success(remotePref)
                } else {
                    Result.Error(IOException("API error: ${response.code()}"))
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }
    
    /**
     * Sync location preferences lên remote
     */
    private suspend fun syncLocationPreferencesToRemote(
        userId: Int,
        locationId: Int,
        notificationsEnabled: Boolean
    ): Result<Unit> {
        return withRetry(MAX_RETRY_ATTEMPTS) {
            try {
                val request = UpdateLocationPreferenceRequest(notificationsEnabled)
                val response = apiService.updateLocationPreferences(locationId, userId, request)
                
                if (response.isSuccessful) {
                    locationPreferencesDao.updateLastSyncedAt(
                        userId,
                        locationId,
                        System.currentTimeMillis()
                    )
                    Result.Success(Unit)
                } else {
                    Result.Error(IOException("API error: ${response.code()}"))
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }

    // ==================== Notification History ====================
    
    /**
     * Lấy notification history với filtering
     * Validates: Requirements 15.1
     */
    suspend fun getNotificationHistory(
        userId: Int,
        notificationType: String? = null,
        startTime: Long? = null,
        endTime: Long? = null
    ): Result<List<NotificationRecord>> {
        return try {
            val records = when {
                notificationType != null && startTime != null && endTime != null -> {
                    recordDao.getRecordsByTypeAndTimeRange(userId, notificationType, startTime, endTime)
                }
                notificationType != null -> {
                    recordDao.getRecordsByType(userId, notificationType)
                }
                startTime != null && endTime != null -> {
                    recordDao.getRecordsByTimeRange(userId, startTime, endTime)
                }
                else -> {
                    recordDao.getAllRecords(userId)
                }
            }
            
            val domainRecords = records.map { NotificationRecord.fromEntity(it) }
            Result.Success(domainRecords)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting notification history", e)
            Result.Error(e, "Không thể tải lịch sử thông báo")
        }
    }
    
    /**
     * Observe notification history với reactive updates
     */
    fun observeNotificationHistory(userId: Int): Flow<List<NotificationRecord>> {
        return recordDao.observeAllRecords(userId).map { entities ->
            entities.map { NotificationRecord.fromEntity(it) }
        }
    }
    
    /**
     * Lưu notification record vào local database
     */
    suspend fun saveNotificationRecord(record: NotificationRecord): Result<Long> {
        return try {
            val id = recordDao.insertRecord(record.toEntity())
            Result.Success(id)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving notification record", e)
            Result.Error(e, "Không thể lưu notification record")
        }
    }
    
    /**
     * Đánh dấu notification đã đọc
     */
    suspend fun markNotificationAsRead(notificationId: Long): Result<Unit> {
        return try {
            recordDao.markAsRead(notificationId, true)
            Result.Success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notification as read", e)
            Result.Error(e, "Không thể cập nhật trạng thái")
        }
    }
    
    /**
     * Lấy số lượng thông báo chưa đọc
     */
    suspend fun getUnreadCount(userId: Int): Result<Int> {
        return try {
            val count = recordDao.getUnreadCount(userId)
            Result.Success(count)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting unread count", e)
            Result.Error(e, "Không thể lấy số thông báo chưa đọc")
        }
    }
    
    /**
     * Observe unread count với reactive updates
     */
    fun observeUnreadCount(userId: Int): Flow<Int> {
        return recordDao.observeUnreadCount(userId)
    }
    
    // ==================== Offline Support & Queue Management ====================
    
    /**
     * Thêm update vào queue để sync sau khi có mạng
     * Validates: Requirements 11.5
     */
    private fun queuePreferenceUpdate(update: PendingPreferenceUpdate) {
        synchronized(pendingUpdates) {
            // Xóa update cũ cho cùng user/location nếu có
            pendingUpdates.removeAll { existing ->
                when {
                    existing is PendingPreferenceUpdate.NotificationPreferencesUpdate &&
                    update is PendingPreferenceUpdate.NotificationPreferencesUpdate ->
                        existing.userId == update.userId
                    
                    existing is PendingPreferenceUpdate.LocationPreferencesUpdate &&
                    update is PendingPreferenceUpdate.LocationPreferencesUpdate ->
                        existing.userId == update.userId && existing.locationId == update.locationId
                    
                    else -> false
                }
            }
            
            pendingUpdates.add(update)
            Log.d(TAG, "Queued preference update. Queue size: ${pendingUpdates.size}")
        }
    }
    
    /**
     * Sync tất cả pending updates khi có mạng trở lại
     * Validates: Requirements 11.5, 15.4
     */
    suspend fun syncPendingUpdates(): Result<Unit> {
        if (!networkMonitor.isNetworkAvailable()) {
            Log.w(TAG, "Cannot sync pending updates: no network available")
            return Result.Error(IOException("No network available"))
        }
        
        val updates = synchronized(pendingUpdates) {
            pendingUpdates.toList()
        }
        
        if (updates.isEmpty()) {
            Log.d(TAG, "No pending updates to sync")
            return Result.Success(Unit)
        }
        
        Log.d(TAG, "Syncing ${updates.size} pending updates")
        var successCount = 0
        var failureCount = 0
        
        for (update in updates) {
            val result = when (update) {
                is PendingPreferenceUpdate.NotificationPreferencesUpdate -> {
                    syncPreferencesToRemote(update.preferences)
                }
                is PendingPreferenceUpdate.LocationPreferencesUpdate -> {
                    syncLocationPreferencesToRemote(
                        update.userId,
                        update.locationId,
                        update.notificationsEnabled
                    )
                }
            }
            
            when (result) {
                is Result.Success<*> -> {
                    successCount++
                    synchronized(pendingUpdates) {
                        pendingUpdates.remove(update)
                    }
                    Log.d(TAG, "Successfully synced pending update")
                }
                is Result.Error -> {
                    failureCount++
                    Log.w(TAG, "Failed to sync pending update: ${result.message}")
                }
                else -> {}
            }
        }
        
        Log.d(TAG, "Sync completed: $successCount succeeded, $failureCount failed")
        
        return if (failureCount == 0) {
            Result.Success(Unit)
        } else {
            Result.Error(
                Exception("Some updates failed to sync"),
                "$failureCount updates không thể đồng bộ"
            )
        }
    }
    
    /**
     * Lấy số lượng updates đang chờ sync
     */
    fun getPendingUpdateCount(): Int {
        return synchronized(pendingUpdates) {
            pendingUpdates.size
        }
    }
    
    /**
     * Observe network status và tự động sync khi có mạng trở lại
     * Validates: Requirements 11.5, 15.4
     */
    fun observeNetworkAndAutoSync(): Flow<Boolean> {
        return networkMonitor.observeNetworkStatus()
    }
    
    // ==================== Helper Functions ====================
    
    /**
     * Retry logic với exponential backoff
     * Validates: Requirements 15.4
     */
    private suspend fun <T> withRetry(
        maxAttempts: Int,
        block: suspend () -> Result<T>
    ): Result<T> {
        var currentAttempt = 0
        var lastException: Exception? = null
        
        while (currentAttempt < maxAttempts) {
            try {
                return block()
            } catch (e: Exception) {
                lastException = e
                currentAttempt++
                
                if (currentAttempt < maxAttempts) {
                    val backoffTime = INITIAL_BACKOFF_MS * (1 shl (currentAttempt - 1))
                    Log.d(TAG, "Retry attempt $currentAttempt after ${backoffTime}ms")
                    delay(backoffTime)
                }
            }
        }
        
        Log.e(TAG, "All retry attempts failed", lastException)
        return Result.Error(
            lastException ?: Exception("Unknown error"),
            "Không thể kết nối sau $maxAttempts lần thử"
        )
    }
}
