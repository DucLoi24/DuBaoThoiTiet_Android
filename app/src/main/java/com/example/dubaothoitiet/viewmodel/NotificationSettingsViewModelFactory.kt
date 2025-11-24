package com.example.dubaothoitiet.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.dubaothoitiet.data.NotificationRepository

/**
 * Factory để tạo NotificationSettingsViewModel với dependencies
 */
class NotificationSettingsViewModelFactory(
    private val repository: NotificationRepository,
    private val userId: Int
) : ViewModelProvider.Factory {
    
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NotificationSettingsViewModel::class.java)) {
            return NotificationSettingsViewModel(repository, userId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
