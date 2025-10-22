package com.example.dubaothoitiet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dubaothoitiet.api.RetrofitInstance
import com.example.dubaothoitiet.data.AuthRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    fun login(username: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitInstance.api.login(AuthRequest(username, password))
                if (response.isSuccessful) {
                    response.body()?.let {
                        // Sửa lỗi: Truy cập vào đối tượng user lồng nhau
                        _authState.value = AuthState.Authenticated(it.user.userId, it.user.username)
                    } ?: run {
                        _authState.value = AuthState.Error("Phản hồi đăng nhập không hợp lệ.")
                    }
                } else {
                    when (response.code()) {
                        401 -> _authState.value = AuthState.Error("Sai tên đăng nhập hoặc mật khẩu")
                        400 -> _authState.value = AuthState.Error("Vui lòng điền đầy đủ thông tin")
                        else -> _authState.value = AuthState.Error("Lỗi không xác định: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Lỗi mạng: ${e.message}")
            }
        }
    }

    fun register(username: String, password: String, confirmPassword: String) {
        if (password != confirmPassword) {
            _authState.value = AuthState.Error("Mật khẩu xác nhận không khớp")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val response = RetrofitInstance.api.register(AuthRequest(username, password))
                if (response.isSuccessful) {
                    // Sau khi đăng ký thành công, tự động đăng nhập
                    login(username, password)
                } else {
                    when (response.code()) {
                        409 -> _authState.value = AuthState.Error("Tên đăng nhập đã tồn tại")
                        400 -> _authState.value = AuthState.Error("Vui lòng điền đầy đủ thông tin")
                        else -> _authState.value = AuthState.Error("Lỗi không xác định: ${response.code()}")
                    }
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error("Lỗi mạng: ${e.message}")
            }
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val userId: Int, val username: String) : AuthState()
    data class Error(val message: String) : AuthState()
}
