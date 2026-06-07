package com.example.notingapp.model

data class SignupRequest(
    val userId: String,
    val displayName: String,
    val password: String
)

data class LoginRequest(
    val userId: String,
    val password: String
)

data class AuthUser(
    val userId: String,
    val displayName: String? = null,
    val avatarColor: String? = null,
    val createdAt: Long? = null,
    val lastLoginAt: Long? = null
)

data class TripMateAuthResponse(
    val message: String? = null,
    val user: AuthUser? = null
)
