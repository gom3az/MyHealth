package com.gomaa.healthy.data.healthkit

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
    val tokenExpiry: Long,
    val userId: String,
    val scopes: String
)