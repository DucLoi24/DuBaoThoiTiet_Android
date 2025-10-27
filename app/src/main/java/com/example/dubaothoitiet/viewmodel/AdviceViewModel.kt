package com.example.dubaothoitiet.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dubaothoitiet.api.RetrofitInstance
import com.example.dubaothoitiet.data.AdviceResponse
import kotlinx.coroutines.launch

// Enum để quản lý trạng thái
sealed class AdviceState {
    object Idle : AdviceState() // Chưa làm gì
    object Loading : AdviceState() // Đang gọi API
    data class Success(val response: AdviceResponse) : AdviceState() // Thành công
    data class Error(val message: String) : AdviceState() // Lỗi
}

class AdviceViewModel : ViewModel() {

    // Sử dụng State Compose thay vì LiveData để dễ quản lý Dialog
    var adviceState by mutableStateOf<AdviceState>(AdviceState.Idle)
        private set

    fun fetchAdvice(locationNameEn: String) {
        viewModelScope.launch {
            adviceState = AdviceState.Loading // Bắt đầu Loading
            try {
                val response = RetrofitInstance.api.getAdvice(locationNameEn)
                if (response.isSuccessful && response.body() != null) {
                    adviceState = AdviceState.Success(response.body()!!)
                } else {
                    // Xử lý lỗi từ API (404, 503, etc.)
                    val errorMsg = response.errorBody()?.string() ?: "Lỗi ${response.code()}"
                    try {
                        // Thử parse lỗi nếu backend trả về JSON lỗi chuẩn
                        val errorResponse = retrofit2.converter.gson.GsonConverterFactory.create()
                            .responseBodyConverter(AdviceResponse::class.java, arrayOfNulls(0), null)
                            ?.convert(response.errorBody()!!) as? AdviceResponse
                        adviceState = AdviceState.Error(errorResponse?.messageVi ?: errorMsg)
                    } catch (e: Exception) {
                        adviceState = AdviceState.Error("Lỗi không xác định từ máy chủ: ${response.code()}")
                    }

                }
            } catch (e: Exception) {
                Log.e("AdviceViewModel", "Network error fetching advice", e)
                adviceState = AdviceState.Error("Lỗi mạng: ${e.localizedMessage}")
            }
        }
    }

    // Hàm để reset trạng thái khi đóng Dialog
    fun dismissAdvice() {
        adviceState = AdviceState.Idle
    }
}