package com.example.dubaothoitiet.data

import com.google.gson.annotations.SerializedName

data class AuthRequest(
    val username: String,
    val password: String
)

data class RegisteredUserResponse(
    @SerializedName("user_id")
    val userId: Int,
    val username: String
)

data class UserData(
    @SerializedName("user_id")
    val userId: Int,
    val username: String
)

data class LoginResponse(
    val message: String,
    val user: UserData
)
