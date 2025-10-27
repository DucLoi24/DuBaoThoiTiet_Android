package com.example.dubaothoitiet.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dubaothoitiet.api.RetrofitInstance
import com.example.dubaothoitiet.data.ExtremeAlert
import kotlinx.coroutines.launch

class AlertViewModel : ViewModel() {

    private val _alerts = MutableLiveData<Result<List<ExtremeAlert>>>()
    val alerts: LiveData<Result<List<ExtremeAlert>>> = _alerts

    fun fetchAlerts(locationNameEn: String) {
        viewModelScope.launch {
            try {
                val response = RetrofitInstance.api.getAlerts(locationNameEn)
                if (response.isSuccessful) {
                    _alerts.postValue(Result.success(response.body() ?: emptyList()))
                } else {
                    // API trả về lỗi (ví dụ 500), coi như không có cảnh báo để tránh crash
                    Log.e("AlertViewModel", "API Error fetching alerts: ${response.code()}")
                    _alerts.postValue(Result.success(emptyList())) // Trả về list rỗng
                }
            } catch (e: Exception) {
                Log.e("AlertViewModel", "Network error fetching alerts", e)
                _alerts.postValue(Result.failure(e)) // Báo lỗi mạng
            }
        }
    }

    // Hàm để xóa cảnh báo khi tìm kiếm địa điểm khác
    fun clearAlerts() {
        _alerts.postValue(Result.success(emptyList()))
    }
}