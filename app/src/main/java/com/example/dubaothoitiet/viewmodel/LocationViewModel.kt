package com.example.dubaothoitiet.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dubaothoitiet.api.RetrofitInstance
import com.example.dubaothoitiet.data.TrackLocationRequest
import kotlinx.coroutines.launch

class LocationViewModel : ViewModel() {

    // Sửa lại hàm để khớp với cách gọi từ MainActivity
    fun trackLocation(
        userId: String, // Nhận userId là String
        name: String,
        nameEn: String,
        lat: Double,
        lon: Double
        // Loại bỏ onResult callback
    ) {
        viewModelScope.launch {
            try {
                // Chuyển đổi userId từ String sang Int, nếu không được thì không làm gì cả
                val userIdInt = userId.toIntOrNull() ?: return@launch

                val request = TrackLocationRequest(
                    userId = userIdInt,
                    name = name,
                    nameEn = nameEn,
                    lat = lat,
                    lon = lon
                )
                val response = RetrofitInstance.api.trackLocation(request)

                if (response.isSuccessful) {
                    Log.d("LocationViewModel", "Location tracked successfully")
                } else {
                    Log.e("LocationViewModel", "Failed to track location: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("LocationViewModel", "Exception while tracking location", e)
            }
        }
    }
}
