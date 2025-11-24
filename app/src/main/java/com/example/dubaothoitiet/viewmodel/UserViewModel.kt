package com.example.dubaothoitiet.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.dubaothoitiet.data.User
import com.example.dubaothoitiet.service.WeatherNotificationService

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
    fun onLoginSuccess(userId: Int, username: String, context: Context) {
        // Chuyển đổi userId từ Int sang String để khớp với lớp User
        _user.value = User(userId.toString(), username)
        
        // Start weather notification service nếu widget được bật
        val sharedPrefs = context.getSharedPreferences("weather_widget_prefs", Context.MODE_PRIVATE)
        val widgetEnabled = sharedPrefs.getBoolean("widget_enabled", true) // Mặc định là bật
        
        if (widgetEnabled) {
            WeatherNotificationService.start(context)
        }
    }

    /**
     * Được gọi khi người dùng đăng xuất.
     */
    fun onLogout(context: Context) {
        // Stop weather notification service
        WeatherNotificationService.stop(context)
        
        _user.value = null
    }
}
