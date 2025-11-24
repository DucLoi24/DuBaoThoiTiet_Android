package com.example.dubaothoitiet.data

/**
 * Sealed class đại diện cho kết quả của một operation
 * Có thể là Success, Error, hoặc Loading
 */
sealed class Result<out T> {
    /**
     * Operation thành công với data
     */
    data class Success<T>(val data: T) : Result<T>()
    
    /**
     * Operation thất bại với exception và message
     */
    data class Error(
        val exception: Exception,
        val message: String? = null
    ) : Result<Nothing>()
    
    /**
     * Operation đang loading
     */
    object Loading : Result<Nothing>()
}
