package com.example.dubaothoitiet.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dubaothoitiet.api.RetrofitInstance
import com.example.dubaothoitiet.data.WeatherResponse
import kotlinx.coroutines.launch

class WeatherViewModel : ViewModel() {

    private val _weatherResult = MutableLiveData<Result<WeatherResponse>>()
    val weatherResult: LiveData<Result<WeatherResponse>> = _weatherResult

    fun getWeather(city: String) {
        viewModelScope.launch {
            try {
                // Sửa lời gọi hàm thành 'getWeather' và truyền đúng tham số
                val response = RetrofitInstance.api.getWeather(city = city, days = 3)
                _weatherResult.postValue(Result.success(response))
            } catch (e: Exception) {
                Log.e("WeatherViewModel", "Error fetching weather data", e)
                _weatherResult.postValue(Result.failure(e))
            }
        }
    }
}
