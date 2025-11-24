package com.example.dubaothoitiet.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dubaothoitiet.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel quản lý state và logic cho màn hình cài đặt thông báo
 * 
 * Validates: Requirements 11.2, 11.3, 11.4
 */
class NotificationSettingsViewModel(
    private val repository: NotificationRepository,
    private val userId: Int
) : ViewModel() {

    companion object {
        private const val TAG = "NotificationSettingsVM"
    }

    // ==================== State Management ====================

    /**
     * StateFlow cho notification preferences
     * Validates: Requirements 11.2
     */
    private val _preferences = MutableStateFlow<NotificationPreferences?>(null)
    val preferences: StateFlow<NotificationPreferences?> = _preferences.asStateFlow()

    /**
     * StateFlow cho sync status
     * Validates: Requirements 11.2
     */
    private val _syncStatus = MutableStateFlow(SyncStatus.IDLE)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    /**
     * StateFlow cho loading state
     */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * StateFlow cho error messages
     * Validates: Requirements 11.3
     */
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /**
     * StateFlow cho success messages
     */
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    /**
     * StateFlow cho validation errors
     * Validates: Requirements 11.3
     */
    private val _validationError = MutableStateFlow<String?>(null)
    val validationError: StateFlow<String?> = _validationError.asStateFlow()

    init {
        // Load preferences khi ViewModel được khởi tạo
        loadPreferences()
        
        // Observe preferences từ repository để có reactive updates
        observePreferences()
    }

    // ==================== Data Loading ====================

    /**
     * Load preferences từ repository
     * Validates: Requirements 11.2
     */
    private fun loadPreferences() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val result = repository.getPreferences(userId)
                
                when (result) {
                    is Result.Success -> {
                        _preferences.value = result.data
                        Log.d(TAG, "Loaded preferences successfully for user $userId")
                    }
                    is Result.Error -> {
                        val errorMsg = result.message ?: "Không thể tải preferences"
                        _errorMessage.value = errorMsg
                        Log.e(TAG, "Error loading preferences: $errorMsg", result.exception)
                        
                        // Nếu không có preferences, tạo mặc định
                        _preferences.value = NotificationPreferences.default(userId)
                    }
                    else -> {
                        _preferences.value = NotificationPreferences.default(userId)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading preferences", e)
                _errorMessage.value = "Đã xảy ra lỗi khi tải preferences"
                _preferences.value = NotificationPreferences.default(userId)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Observe preferences từ repository để có reactive updates
     */
    private fun observePreferences() {
        viewModelScope.launch {
            repository.observePreferences(userId).collect { prefs ->
                if (prefs != null) {
                    _preferences.value = prefs
                    Log.d(TAG, "Preferences updated from repository")
                }
            }
        }
    }

    // ==================== Preference Updates ====================

    /**
     * Toggle một loại sự kiện thời tiết
     * Validates: Requirements 11.2, 11.3
     */
    fun toggleEventType(eventType: WeatherEventType, enabled: Boolean) {
        viewModelScope.launch {
            val currentPrefs = _preferences.value ?: return@launch
            
            try {
                // Validate: Phải có ít nhất 1 event type được bật
                val newEventTypes = if (enabled) {
                    currentPrefs.enabledEventTypes + eventType
                } else {
                    currentPrefs.enabledEventTypes - eventType
                }
                
                if (newEventTypes.isEmpty()) {
                    _validationError.value = "Phải bật ít nhất một loại thông báo"
                    Log.w(TAG, "Validation failed: Cannot disable all event types")
                    return@launch
                }
                
                _validationError.value = null
                
                val updatedPrefs = currentPrefs.copy(enabledEventTypes = newEventTypes)
                
                Log.d(TAG, "Toggling event type $eventType to $enabled")
                
                // Sync với backend TRƯỚC
                _syncStatus.value = SyncStatus.SYNCING
                val result = repository.updatePreferences(updatedPrefs)
                
                when (result) {
                    is Result.Success -> {
                        // CHỈ update local state KHI backend thành công
                        _preferences.value = updatedPrefs
                        _syncStatus.value = SyncStatus.SUCCESS
                        _successMessage.value = "Đã cập nhật loại thông báo"
                        clearSuccessMessageAfterDelay()
                    }
                    is Result.Error -> {
                        _syncStatus.value = SyncStatus.ERROR
                        val errorMsg = result.message ?: "Không thể cập nhật loại thông báo"
                        _errorMessage.value = errorMsg
                        Log.e(TAG, "Error toggling event type: $errorMsg", result.exception)
                    }
                    else -> {
                        _syncStatus.value = SyncStatus.IDLE
                    }
                }
                
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.ERROR
                Log.e(TAG, "Error toggling event type", e)
                _errorMessage.value = "Không thể cập nhật loại thông báo"
            }
        }
    }

    /**
     * Cập nhật lịch trình thông báo
     * Validates: Requirements 11.2, 11.3
     */
    fun updateSchedule(schedule: NotificationSchedule) {
        viewModelScope.launch {
            val currentPrefs = _preferences.value ?: return@launch
            
            try {
                // Validate schedule
                if (!isValidSchedule(schedule)) {
                    _validationError.value = "Lịch trình không hợp lệ"
                    Log.w(TAG, "Validation failed: Invalid schedule")
                    return@launch
                }
                
                _validationError.value = null
                
                val updatedPrefs = currentPrefs.copy(notificationSchedule = schedule)
                
                Log.d(TAG, "Updating schedule to $schedule")
                
                // Sync với backend TRƯỚC
                _syncStatus.value = SyncStatus.SYNCING
                val result = repository.updatePreferences(updatedPrefs)
                
                when (result) {
                    is Result.Success -> {
                        // CHỈ update local state KHI backend thành công
                        _preferences.value = updatedPrefs
                        _syncStatus.value = SyncStatus.SUCCESS
                        _successMessage.value = "Đã cập nhật lịch trình"
                        clearSuccessMessageAfterDelay()
                    }
                    is Result.Error -> {
                        _syncStatus.value = SyncStatus.ERROR
                        val errorMsg = result.message ?: "Không thể cập nhật lịch trình"
                        _errorMessage.value = errorMsg
                        Log.e(TAG, "Error updating schedule: $errorMsg", result.exception)
                    }
                    else -> {
                        _syncStatus.value = SyncStatus.IDLE
                    }
                }
                
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.ERROR
                Log.e(TAG, "Error updating schedule", e)
                _errorMessage.value = "Không thể cập nhật lịch trình"
            }
        }
    }

    /**
     * Toggle scheduled notification (morning summary, tomorrow forecast, weekly summary)
     * Validates: Requirements 11.2
     */
    fun toggleScheduledNotification(type: String, enabled: Boolean) {
        viewModelScope.launch {
            val currentPrefs = _preferences.value ?: return@launch
            
            try {
                val updatedPrefs = when (type) {
                    "morning_summary" -> currentPrefs.copy(morningSummaryEnabled = enabled)
                    "tomorrow_forecast" -> currentPrefs.copy(tomorrowForecastEnabled = enabled)
                    "weekly_summary" -> currentPrefs.copy(weeklySummaryEnabled = enabled)
                    else -> {
                        Log.w(TAG, "Unknown scheduled notification type: $type")
                        return@launch
                    }
                }
                
                Log.d(TAG, "Toggling scheduled notification $type to $enabled")
                
                // Sync với backend TRƯỚC
                _syncStatus.value = SyncStatus.SYNCING
                val result = repository.updatePreferences(updatedPrefs)
                
                when (result) {
                    is Result.Success -> {
                        // CHỈ update local state KHI backend thành công
                        _preferences.value = updatedPrefs
                        _syncStatus.value = SyncStatus.SUCCESS
                        _successMessage.value = "Đã cập nhật thông báo định kỳ"
                        clearSuccessMessageAfterDelay()
                    }
                    is Result.Error -> {
                        _syncStatus.value = SyncStatus.ERROR
                        val errorMsg = result.message ?: "Không thể cập nhật thông báo định kỳ"
                        _errorMessage.value = errorMsg
                        Log.e(TAG, "Error toggling scheduled notification: $errorMsg", result.exception)
                    }
                    else -> {
                        _syncStatus.value = SyncStatus.IDLE
                    }
                }
                
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.ERROR
                Log.e(TAG, "Error toggling scheduled notification", e)
                _errorMessage.value = "Không thể cập nhật thông báo định kỳ"
            }
        }
    }

    /**
     * Toggle thông báo cho một location cụ thể
     * Validates: Requirements 11.2
     */
    fun toggleLocationNotifications(locationId: Int, enabled: Boolean) {
        viewModelScope.launch {
            _syncStatus.value = SyncStatus.SYNCING
            
            try {
                val result = repository.updateLocationPreferences(userId, locationId, enabled)
                
                when (result) {
                    is Result.Success -> {
                        _syncStatus.value = SyncStatus.SUCCESS
                        _successMessage.value = "Đã cập nhật thông báo cho vị trí"
                        Log.d(TAG, "Updated location notifications for location $locationId")
                        
                        // Clear success message sau 3 giây
                        clearSuccessMessageAfterDelay()
                    }
                    is Result.Error -> {
                        _syncStatus.value = SyncStatus.ERROR
                        val errorMsg = result.message ?: "Không thể cập nhật thông báo vị trí"
                        _errorMessage.value = errorMsg
                        Log.e(TAG, "Error updating location notifications: $errorMsg", result.exception)
                    }
                    else -> {
                        _syncStatus.value = SyncStatus.IDLE
                    }
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.ERROR
                Log.e(TAG, "Exception updating location notifications", e)
                _errorMessage.value = "Đã xảy ra lỗi khi cập nhật thông báo vị trí"
            }
        }
    }

    // ==================== Backend Sync ====================

    /**
     * Manually trigger sync với backend
     * Validates: Requirements 11.2
     */
    fun syncWithBackend() {
        viewModelScope.launch {
            _isLoading.value = true
            _syncStatus.value = SyncStatus.SYNCING
            _errorMessage.value = null
            
            try {
                val result = repository.syncPreferences(userId)
                
                when (result) {
                    is Result.Success -> {
                        _syncStatus.value = SyncStatus.SUCCESS
                        _successMessage.value = "Đã đồng bộ thành công"
                        Log.d(TAG, "Manual sync completed successfully")
                        
                        // Reload preferences sau khi sync
                        loadPreferences()
                        
                        // Clear success message sau 3 giây
                        clearSuccessMessageAfterDelay()
                    }
                    is Result.Error -> {
                        _syncStatus.value = SyncStatus.ERROR
                        val errorMsg = result.message ?: "Không thể đồng bộ với server"
                        _errorMessage.value = errorMsg
                        Log.e(TAG, "Manual sync failed: $errorMsg", result.exception)
                    }
                    else -> {
                        _syncStatus.value = SyncStatus.IDLE
                    }
                }
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.ERROR
                Log.e(TAG, "Exception during manual sync", e)
                _errorMessage.value = "Đã xảy ra lỗi khi đồng bộ"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // ==================== Validation ====================

    /**
     * Validate notification schedule
     * Validates: Requirements 11.3
     */
    private fun isValidSchedule(@Suppress("UNUSED_PARAMETER") schedule: NotificationSchedule): Boolean {
        // Tất cả các giá trị enum đều hợp lệ
        return true
    }

    /**
     * Validate preferences trước khi save
     * Validates: Requirements 11.3
     */
    fun validatePreferences(): Boolean {
        val prefs = _preferences.value ?: return false
        
        // Phải có ít nhất 1 event type được bật
        if (prefs.enabledEventTypes.isEmpty()) {
            _validationError.value = "Phải bật ít nhất một loại thông báo"
            return false
        }
        
        // Timezone không được rỗng
        if (prefs.timezone.isBlank()) {
            _validationError.value = "Timezone không hợp lệ"
            return false
        }
        
        _validationError.value = null
        return true
    }

    // ==================== UI State Management ====================

    /**
     * Clear error message
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * Clear success message
     */
    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    /**
     * Clear validation error
     */
    fun clearValidationError() {
        _validationError.value = null
    }

    /**
     * Clear success message sau một khoảng thời gian
     */
    private fun clearSuccessMessageAfterDelay() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _successMessage.value = null
            _syncStatus.value = SyncStatus.IDLE
        }
    }

    /**
     * Refresh preferences từ backend
     */
    fun refresh() {
        loadPreferences()
    }

    /**
     * Reset preferences về mặc định
     */
    fun resetToDefaults() {
        viewModelScope.launch {
            try {
                val defaultPrefs = NotificationPreferences.default(userId)
                
                Log.d(TAG, "Resetting preferences to defaults")
                
                // Sync với backend TRƯỚC
                _syncStatus.value = SyncStatus.SYNCING
                val result = repository.updatePreferences(defaultPrefs)
                
                when (result) {
                    is Result.Success -> {
                        // CHỈ update local state KHI backend thành công
                        _preferences.value = defaultPrefs
                        _syncStatus.value = SyncStatus.SUCCESS
                        _successMessage.value = "Đã reset về mặc định"
                        clearSuccessMessageAfterDelay()
                    }
                    is Result.Error -> {
                        _syncStatus.value = SyncStatus.ERROR
                        val errorMsg = result.message ?: "Không thể reset preferences"
                        _errorMessage.value = errorMsg
                        Log.e(TAG, "Error resetting preferences: $errorMsg", result.exception)
                    }
                    else -> {
                        _syncStatus.value = SyncStatus.IDLE
                    }
                }
                
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.ERROR
                Log.e(TAG, "Error resetting preferences", e)
                _errorMessage.value = "Không thể reset preferences"
            }
        }
    }

    /**
     * Toggle bật/tắt thông báo tổng thể
     * Validates: Requirements 11.1
     */
    fun toggleNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val currentPrefs = _preferences.value ?: return@launch
            
            // Validate
            if (!enabled && currentPrefs.notificationsEnabled) {
                // Tắt thông báo
                Log.d(TAG, "Disabling all notifications for user $userId")
            } else if (enabled && !currentPrefs.notificationsEnabled) {
                // Bật thông báo
                Log.d(TAG, "Enabling notifications for user $userId")
            }
            
            // Sync với backend TRƯỚC KHI update local state
            _isLoading.value = true
            
            val updatedPrefs = currentPrefs.copy(notificationsEnabled = enabled)
            
            try {
                val result = repository.updatePreferences(updatedPrefs)
                
                when (result) {
                    is Result.Success -> {
                        // CHỈ update local state KHI backend thành công
                        _preferences.value = updatedPrefs
                        _successMessage.value = if (enabled) {
                            "Đã bật thông báo"
                        } else {
                            "Đã tắt thông báo"
                        }
                        Log.d(TAG, "Notifications ${if (enabled) "enabled" else "disabled"} successfully")
                    }
                    is Result.Error -> {
                        // KHÔNG update local state nếu backend thất bại
                        val errorMsg = result.message ?: "Không thể cập nhật cài đặt"
                        _errorMessage.value = errorMsg
                        Log.e(TAG, "Error toggling notifications: $errorMsg", result.exception)
                    }
                    is Result.Loading -> {
                        // Do nothing
                    }
                }
            } catch (e: Exception) {
                // KHÔNG update local state nếu có exception
                _errorMessage.value = "Lỗi: ${e.message}"
                Log.e(TAG, "Exception toggling notifications", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
