package com.example.dubaothoitiet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.dubaothoitiet.data.NotificationRepository

/**
 * Factory để tạo TrackedLocationsViewModel với dependencies
 * 
 * Validates: Requirements 13.1, 13.3, 13.4, 13.5
 */
class TrackedLocationsViewModelFactory(
    private val repository: NotificationRepository
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrackedLocationsViewModel::class.java)) {
            return TrackedLocationsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
