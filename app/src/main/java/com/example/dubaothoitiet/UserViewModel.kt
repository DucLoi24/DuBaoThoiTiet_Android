package com.example.dubaothoitiet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * ViewModel để chia sẻ trạng thái đăng nhập của người dùng trên toàn ứng dụng.
 */
class UserViewModel : ViewModel() {
    var userId by mutableStateOf<Int?>(null)
        private set

    var username by mutableStateOf<String?>(null)
        private set

    val isLoggedIn: Boolean
        get() = username != null && userId != null

    fun onLoginSuccess(id: Int, name: String) {
        userId = id
        username = name
    }

    fun onLogout() {
        userId = null
        username = null
    }
}
