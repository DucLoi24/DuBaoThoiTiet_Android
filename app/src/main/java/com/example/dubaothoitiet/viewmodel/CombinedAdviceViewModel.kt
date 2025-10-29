package com.example.dubaothoitiet.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dubaothoitiet.api.RetrofitInstance
import com.example.dubaothoitiet.data.CheckAdviceResponse // Import data class mới
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

// State mới quản lý cả 3 trạng thái: Loading, Có Advice, Cần Bấm Nút
sealed class CombinedAdviceUiState {
    object Loading : CombinedAdviceUiState() // Đang kiểm tra hoặc đang lấy advice mới
    data class Success(val adviceData: CheckAdviceResponse) : CombinedAdviceUiState() // Có advice/warning để hiển thị
    object Stale : CombinedAdviceUiState() // Cần bấm nút để tạo advice mới
    data class Error(val message: String) : CombinedAdviceUiState() // Lỗi
    object Idle : CombinedAdviceUiState() // Trạng thái ban đầu
}

class CombinedAdviceViewModel : ViewModel() {

    var uiState by mutableStateOf<CombinedAdviceUiState>(CombinedAdviceUiState.Idle)
        private set
    private var pollingJob: Job? = null

    // Hàm kiểm tra advice gần đây (gọi API /check-advice/)
    fun checkOrFetchAdvice(locationNameEn: String) {
        if (locationNameEn.isBlank()) return // Bỏ qua nếu tên rỗng
        viewModelScope.launch {
            uiState = CombinedAdviceUiState.Loading
            try {
                val response = RetrofitInstance.api.checkAdvice(locationNameEn)
                if (response.isSuccessful && response.body() != null) {
                    val checkResponse = response.body()!!
                    if (checkResponse.hasValidAdvice()) {
                        uiState = CombinedAdviceUiState.Success(checkResponse)
                    } else { // status == "stale"
                        uiState = CombinedAdviceUiState.Stale
                    }
                } else {
                    Log.e("CombinedAdviceVM", "API Error checkAdvice: ${response.code()}")
                    uiState = CombinedAdviceUiState.Error("Lỗi ${response.code()} khi kiểm tra lời khuyên.")
                }
            } catch (e: Exception) {
                Log.e("CombinedAdviceVM", "Network error checkAdvice", e)
                uiState = CombinedAdviceUiState.Error("Lỗi mạng khi kiểm tra lời khuyên.")
            }
        }
    }

    // Hàm gọi API /advice/ khi người dùng bấm nút
    fun generateNewAdvice(locationNameEn: String) {
        if (locationNameEn.isBlank() || uiState == CombinedAdviceUiState.Loading) return // Tránh gọi liên tục khi đang loading

        // Hủy job polling cũ nếu đang chạy
        pollingJob?.cancel()

        viewModelScope.launch {
            uiState = CombinedAdviceUiState.Loading // Bắt đầu Loading

            // 1. Kích hoạt API /advice/ (không cần chờ kết quả ở đây)
            try {
                // Không cần gán response, chỉ cần gọi để kích hoạt backend
                RetrofitInstance.api.getAdvice(locationNameEn)
                Log.d("CombinedAdviceVM", "Triggered /api/advice/ successfully.")
            } catch (e: Exception) {
                // Lỗi ngay khi kích hoạt (ví dụ: mạng yếu)
                Log.e("CombinedAdviceVM", "Error triggering /api/advice/", e)
                uiState = CombinedAdviceUiState.Error("Lỗi khi yêu cầu lời khuyên. Vui lòng thử lại.")
                return@launch // Dừng lại nếu không kích hoạt được
            }

            // 2. Bắt đầu polling /check-advice/
            pollingJob = viewModelScope.launch {
                var attempts = 0
                val maxAttempts = 12 // Tối đa 12 lần thử (khoảng 1 phút nếu delay 5s)
                var foundResult = false

                while (attempts < maxAttempts && !foundResult) {
                    delay(5000) // Chờ 5 giây
                    attempts++
                    Log.d("CombinedAdviceVM", "Polling attempt $attempts...")

                    try {
                        val checkResponse = RetrofitInstance.api.checkAdvice(locationNameEn)
                        if (checkResponse.isSuccessful && checkResponse.body() != null) {
                            val result = checkResponse.body()!!
                            if (result.hasValidAdvice()) {
                                uiState = CombinedAdviceUiState.Success(result)
                                foundResult = true // Đã tìm thấy, dừng polling
                                Log.i("CombinedAdviceVM", "Polling successful!")
                            } else {
                                // Vẫn là stale, tiếp tục polling
                                Log.d("CombinedAdviceVM", "Still stale...")
                            }
                        } else {
                            // Lỗi API /check-advice/
                            Log.e("CombinedAdviceVM", "Polling error: ${checkResponse.code()}")
                            // Có thể dừng polling và báo lỗi nếu muốn
                            // uiState = CombinedAdviceUiState.Error("Lỗi khi kiểm tra kết quả.")
                            // break
                        }
                    } catch (e: Exception) {
                        // Lỗi mạng khi polling
                        Log.e("CombinedAdviceVM", "Polling network error", e)
                        // Có thể dừng polling và báo lỗi nếu muốn
                        // uiState = CombinedAdviceUiState.Error("Lỗi mạng khi kiểm tra kết quả.")
                        // break
                    }
                } // Hết while

                // Nếu hết attempts mà vẫn chưa thấy kết quả
                if (!foundResult && uiState == CombinedAdviceUiState.Loading) {
                    Log.w("CombinedAdviceVM", "Polling timed out after $maxAttempts attempts.")
                    uiState = CombinedAdviceUiState.Error("AI xử lý quá lâu hoặc đã xảy ra lỗi. Vui lòng thử lại.")
                }
            } // Hết pollingJob launch
        } // Hết generateNewAdvice launch
    }

    // Hàm để ẩn hoặc reset
    override fun onCleared() {
        super.onCleared()
        pollingJob?.cancel()
    }

    // Sửa lại hàm dismiss/reset để cancel polling nếu cần
    fun dismiss() {
        pollingJob?.cancel() // Dừng polling nếu người dùng ẩn kết quả
        uiState = CombinedAdviceUiState.Idle
    }
    fun resetState() {
        pollingJob?.cancel() // Dừng polling khi tìm địa điểm khác
        uiState = CombinedAdviceUiState.Idle
    }
}