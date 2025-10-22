package com.example.dubaothoitiet.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dubaothoitiet.data.User // Import lớp User mới

class UserViewModel : ViewModel() {

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    init {
        // Khởi tạo không có người dùng nào đăng nhập
        _user.value = null
    }

    /**
     * Được gọi khi người dùng đăng nhập thành công.
     */
    fun onLoginSuccess(userId: Int, username: String) {
        // Chuyển đổi userId từ Int sang String để khớp với lớp User
        _user.value = User(userId.toString(), username)
    }

    /**
     * Được gọi khi người dùng đăng xuất.
     */
    fun onLogout() {
        _user.value = null
    }
}
