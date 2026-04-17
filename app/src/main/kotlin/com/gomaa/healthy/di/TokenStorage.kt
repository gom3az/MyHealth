package com.gomaa.healthy.di

import com.gomaa.healthy.data.healthkit.AuthState
import com.gomaa.healthy.data.healthkit.AuthTokens
import kotlinx.coroutines.flow.Flow

interface TokenStorage {
    suspend fun saveTokens(tokens: AuthTokens)
    suspend fun loadTokens(): AuthTokens?
    suspend fun clearTokens()
    suspend fun getAccessToken(): String?
    suspend fun getTokenExpiry(): Long
    suspend fun getRefreshToken(): String?
    fun authStateFlow(): Flow<AuthState>
}