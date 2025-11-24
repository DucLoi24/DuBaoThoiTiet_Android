package com.example.dubaothoitiet.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dubaothoitiet.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * ViewModel cho màn hình lịch sử thông báo
 * 
 * Validates: Requirements 14.1, 14.2, 14.3, 14.4, 14.5
 */
class NotificationHistoryViewModel(
    private val repository: NotificationRepository,
    private val userId: Int
) : ViewModel() {

    companion object {
        private const val TAG = "NotificationHistoryVM"
    }

    // State flows
    private val _notifications = MutableStateFlow<List<NotificationRecord>>(emptyList())
    val notifications: StateFlow<List<NotificationRecord>> = _notifications.asStateFlow()

    private val _filteredNotifications = MutableStateFlow<List<NotificationRecord>>(emptyList())
    val filteredNotifications: StateFlow<List<NotificationRecord>> = _filteredNotifications.asStateFlow()

    private val _selectedNotification = MutableStateFlow<NotificationRecord?>(null)
    val selectedNotification: StateFlow<NotificationRecord?> = _selectedNotification.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    // Filter states
    private val _selectedType = MutableStateFlow<NotificationType?>(null)
    val selectedType: StateFlow<NotificationType?> = _selectedType.asStateFlow()

    private val _startDate = MutableStateFlow<Long?>(null)
    val startDate: StateFlow<Long?> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow<Long?>(null)
    val endDate: StateFlow<Long?> = _endDate.asStateFlow()

    init {
        loadNotifications()
        observeUnreadCount()
    }

    /**
     * Load notification history từ repository
     * Validates: Requirements 14.1
     */
    private fun loadNotifications() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Fetch từ API trước
                val result = repository.getNotificationHistory(userId)
                
                when (result) {
                    is Result.Success -> {
                        val sorted = result.data.sortedByDescending { it.receivedAt }
                        _notifications.value = sorted
                        applyFilters()
                        Log.d(TAG, "Loaded ${sorted.size} notifications from API")
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Error loading notifications: ${result.message}", result.exception)
                        _errorMessage.value = result.message ?: "Không thể tải lịch sử thông báo"
                        // Nếu lỗi, vẫn hiển thị empty state thay vì loading mãi
                        _notifications.value = emptyList()
                        applyFilters()
                    }
                    is Result.Loading -> {
                        // Do nothing, already loading
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception loading notifications", e)
                _errorMessage.value = "Không thể tải lịch sử thông báo"
                _notifications.value = emptyList()
                applyFilters()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Observe unread count
     */
    private fun observeUnreadCount() {
        viewModelScope.launch {
            repository.observeUnreadCount(userId)
                .collect { count ->
                    _unreadCount.value = count
                }
        }
    }

    /**
     * Refresh notifications từ repository
     */
    fun refresh() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.getNotificationHistory(
                    userId = userId,
                    notificationType = _selectedType.value?.apiValue,
                    startTime = _startDate.value,
                    endTime = _endDate.value
                )

                when (result) {
                    is Result.Success -> {
                        val sorted = result.data.sortedByDescending { it.receivedAt }
                        _notifications.value = sorted
                        applyFilters()
                        Log.d(TAG, "Refreshed ${sorted.size} notifications")
                    }
                    is Result.Error -> {
                        Log.e(TAG, "Error refreshing notifications", result.exception)
                        _errorMessage.value = result.message ?: "Không thể tải lịch sử"
                    }
                    else -> {}
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Select notification để xem chi tiết
     * Validates: Requirements 14.3
     */
    fun selectNotification(notification: NotificationRecord) {
        _selectedNotification.value = notification
        
        // Mark as read nếu chưa đọc
        if (!notification.read) {
            markAsRead(notification.id)
        }
    }

    /**
     * Clear selected notification
     */
    fun clearSelection() {
        _selectedNotification.value = null
    }

    /**
     * Mark notification as read
     */
    private fun markAsRead(notificationId: Long) {
        viewModelScope.launch {
            try {
                repository.markNotificationAsRead(notificationId)
                Log.d(TAG, "Marked notification $notificationId as read")
            } catch (e: Exception) {
                Log.e(TAG, "Error marking notification as read", e)
            }
        }
    }

    /**
     * Filter by notification type
     * Validates: Requirements 14.4
     */
    fun filterByType(type: NotificationType?) {
        _selectedType.value = type
        applyFilters()
    }

    /**
     * Filter by date range
     * Validates: Requirements 14.4
     */
    fun filterByDateRange(startDate: Long?, endDate: Long?) {
        _startDate.value = startDate
        _endDate.value = endDate
        applyFilters()
    }

    /**
     * Clear all filters
     */
    fun clearFilters() {
        _selectedType.value = null
        _startDate.value = null
        _endDate.value = null
        applyFilters()
    }

    /**
     * Apply current filters to notifications list
     * Validates: Requirements 14.4
     */
    private fun applyFilters() {
        var filtered = _notifications.value

        // Filter by type
        _selectedType.value?.let { type ->
            filtered = filtered.filter { it.notificationType == type }
        }

        // Filter by date range
        val start = _startDate.value
        val end = _endDate.value
        
        if (start != null || end != null) {
            filtered = filtered.filter { notification ->
                val timestamp = notification.receivedAt
                val afterStart = start == null || timestamp >= start
                val beforeEnd = end == null || timestamp <= end
                afterStart && beforeEnd
            }
        }

        _filteredNotifications.value = filtered
        Log.d(TAG, "Applied filters: ${filtered.size} notifications")
    }

    /**
     * Get quick filter options
     */
    fun getQuickFilterOptions(): List<QuickFilter> {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        return listOf(
            QuickFilter(
                label = "Hôm nay",
                startTime = calendar.apply {
                    timeInMillis = now
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                }.timeInMillis,
                endTime = now
            ),
            QuickFilter(
                label = "7 ngày qua",
                startTime = now - (7 * 24 * 60 * 60 * 1000L),
                endTime = now
            ),
            QuickFilter(
                label = "30 ngày qua",
                startTime = now - (30 * 24 * 60 * 60 * 1000L),
                endTime = now
            )
        )
    }

    /**
     * Clear error message
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }
}

/**
 * Quick filter option
 */
data class QuickFilter(
    val label: String,
    val startTime: Long,
    val endTime: Long
)


/**
 * Factory cho NotificationHistoryViewModel
 */
class NotificationHistoryViewModelFactory(
    private val repository: NotificationRepository,
    private val userId: Int
) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return NotificationHistoryViewModel(repository, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
