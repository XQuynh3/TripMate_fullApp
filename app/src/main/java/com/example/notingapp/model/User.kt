package com.example.notingapp.model

data class User(
    val id: String,
    val email: String,
    val token: String? = null
)

data class AuthResponse(
    val message: String,
    val user: User?,
    val token: String?
)
