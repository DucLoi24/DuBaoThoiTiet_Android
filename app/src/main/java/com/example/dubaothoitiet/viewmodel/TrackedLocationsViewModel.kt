package com.example.dubaothoitiet.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dubaothoitiet.api.RetrofitInstance
import com.example.dubaothoitiet.data.LocationNotificationPreferences
import com.example.dubaothoitiet.data.NotificationRepository
import com.example.dubaothoitiet.data.Result
import com.example.dubaothoitiet.data.TrackedLocation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TrackedLocationsViewModel(
    private val notificationRepository: NotificationRepository
) : ViewModel() {
    
    private val _trackedLocations = MutableLiveData<List<TrackedLocation>>()
    val trackedLocations: LiveData<List<TrackedLocation>> = _trackedLocations
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    // Location notification preferences state
    private val _locationPreferences = MutableStateFlow<Map<Int, LocationNotificationPreferences>>(emptyMap())
    val locationPreferences: StateFlow<Map<Int, LocationNotificationPreferences>> = _locationPreferences.asStateFlow()
    
    // Delete confirmation state
    private val _showDeleteDialog = MutableStateFlow<TrackedLocation?>(null)
    val showDeleteDialog: StateFlow<TrackedLocation?> = _showDeleteDialog.asStateFlow()
    
    // Success message state
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    
    private var currentUserId: Int? = null
    
    fun loadTrackedLocations(userId: String) {
        val userIdInt = userId.toIntOrNull()
        if (userIdInt == null) {
            _error.value = "Invalid user ID"
            return
        }
        
        currentUserId = userIdInt
        
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                // Load tracked locations
                val response = RetrofitInstance.api.getTrackedLocations(userIdInt)
                
                if (response.isSuccessful && response.body() != null) {
                    _trackedLocations.value = response.body()!!
                    Log.d("TrackedLocationsVM", "Loaded ${response.body()!!.size} tracked locations")
                    
                    // Load notification preferences for each location
                    loadLocationPreferences(userIdInt)
                } else {
                    _error.value = "Không thể tải danh sách vị trí (${response.code()})"
                    Log.e("TrackedLocationsVM", "Failed to load: ${response.code()}")
                }
            } catch (e: Exception) {
                _error.value = "Lỗi mạng: ${e.message}"
                Log.e("TrackedLocationsVM", "Exception loading tracked locations", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    private suspend fun loadLocationPreferences(userId: Int) {
        val locations = _trackedLocations.value ?: return
        val prefsMap = mutableMapOf<Int, LocationNotificationPreferences>()
        
        for (location in locations) {
            when (val result = notificationRepository.getLocationPreferences(userId, location.id)) {
                is Result.Success -> {
                    prefsMap[location.id] = result.data
                }
                is Result.Error -> {
                    Log.w("TrackedLocationsVM", "Failed to load preferences for location ${location.id}: ${result.message}")
                    // Create default preferences if not found
                    prefsMap[location.id] = LocationNotificationPreferences(
                        userId = userId,
                        locationId = location.id,
                        notificationsEnabled = true
                    )
                }
                is Result.Loading -> {}
            }
        }
        
        _locationPreferences.value = prefsMap
    }
    

    /**
     * Show delete confirmation dialog
     * Validates: Requirements 13.5
     */
    fun showDeleteConfirmation(location: TrackedLocation) {
        _showDeleteDialog.value = location
    }
    
    /**
     * Hide delete confirmation dialog
     */
    fun hideDeleteConfirmation() {
        _showDeleteDialog.value = null
    }
    
    /**
     * Delete a tracked location and its preferences
     * Validates: Requirements 13.5
     */
    fun deleteLocation(location: TrackedLocation) {
        val userId = currentUserId ?: return
        
        viewModelScope.launch {
            try {
                // Delete from backend
                val response = RetrofitInstance.api.deleteTrackedLocation(userId, location.id)
                
                if (response.isSuccessful) {
                    // Remove from local state
                    val currentLocations = _trackedLocations.value?.toMutableList() ?: mutableListOf()
                    currentLocations.removeAll { it.id == location.id }
                    _trackedLocations.value = currentLocations
                    
                    // Remove preferences from local state
                    val currentPrefs = _locationPreferences.value.toMutableMap()
                    currentPrefs.remove(location.id)
                    _locationPreferences.value = currentPrefs
                    
                    _successMessage.value = "Đã xóa vị trí ${location.name}"
                    _showDeleteDialog.value = null
                    
                    Log.d("TrackedLocationsVM", "Deleted location ${location.id}")
                } else {
                    _error.value = "Không thể xóa vị trí (${response.code()})"
                    Log.e("TrackedLocationsVM", "Failed to delete location: ${response.code()}")
                }
            } catch (e: Exception) {
                _error.value = "Lỗi: ${e.message}"
                Log.e("TrackedLocationsVM", "Exception deleting location", e)
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun clearSuccessMessage() {
        _successMessage.value = null
    }
    
    /**
     * Lấy weather data cho biểu đồ
     * Gọi API với aqi=yes để có đầy đủ dữ liệu UV và AQI
     */
    private val _weatherDataCache = MutableStateFlow<Map<String, com.example.dubaothoitiet.data.WeatherResponse>>(emptyMap())
    
    fun getWeatherDataForLocation(locationName: String): com.example.dubaothoitiet.data.WeatherResponse? {
        // Trả về từ cache nếu có
        val cached = _weatherDataCache.value[locationName]
        if (cached != null) {
            return cached
        }
        
        // Nếu chưa có, fetch từ API
        viewModelScope.launch {
            try {
                val weatherData = RetrofitInstance.api.getWeather(
                    city = locationName,
                    days = 1,
                    aqi = "yes",  // Quan trọng: Phải có aqi=yes để lấy dữ liệu AQI
                    alerts = "yes"
                )
                
                // Lưu vào cache
                val currentCache = _weatherDataCache.value.toMutableMap()
                currentCache[locationName] = weatherData
                _weatherDataCache.value = currentCache
                
                Log.d("TrackedLocationsVM", "Fetched weather data for $locationName with AQI")
            } catch (e: Exception) {
                Log.e("TrackedLocationsVM", "Error fetching weather data for charts", e)
            }
        }
        
        return null
    }
    
    /**
     * Toggle thông báo cho một vị trí cụ thể
     * Validates: Requirements 4.2, 4.3, 13.4
     */
    fun toggleLocationNotification(locationId: Int, enabled: Boolean) {
        viewModelScope.launch {
            _isLoading.value = true
            
            try {
                val userIdInt = currentUserId
                if (userIdInt == null) {
                    _error.value = "User ID không hợp lệ"
                    _isLoading.value = false
                    return@launch
                }
                
                when (val result = notificationRepository.updateLocationPreferences(
                    userId = userIdInt,
                    locationId = locationId,
                    notificationsEnabled = enabled
                )) {
                    is Result.Success -> {
                        // Update local state
                        val currentPrefs = _locationPreferences.value.toMutableMap()
                        currentPrefs[locationId] = LocationNotificationPreferences(
                            userId = userIdInt,
                            locationId = locationId,
                            notificationsEnabled = enabled
                        )
                        _locationPreferences.value = currentPrefs
                        
                        _successMessage.value = if (enabled) {
                            "Đã bật thông báo cho vị trí này"
                        } else {
                            "Đã tắt thông báo cho vị trí này"
                        }
                        
                        Log.d("TrackedLocationsVM", "Location notification ${if (enabled) "enabled" else "disabled"} for location $locationId")
                    }
                    is Result.Error -> {
                        val errorMsg = result.message ?: "Không thể cập nhật cài đặt thông báo"
                        _error.value = errorMsg
                        Log.e("TrackedLocationsVM", "Error toggling location notification: $errorMsg", result.exception)
                    }
                    is Result.Loading -> {
                        // Do nothing, already loading
                    }
                }
            } catch (e: Exception) {
                _error.value = "Lỗi: ${e.message}"
                Log.e("TrackedLocationsVM", "Exception toggling location notification", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

}
