package com.gomaa.healthy.di

import com.gomaa.healthy.data.healthkit.AuthState
import com.gomaa.healthy.data.healthkit.AuthTokens
import kotlinx.coroutines.flow.Flow

interface TokenStorage {
    fun saveTokens(tokens: AuthTokens)
    fun loadTokens(): AuthTokens?
    fun clearTokens()
    fun getAccessToken(): String?
    fun getTokenExpiry(): Long
    fun getRefreshToken(): String?
    fun authStateFlow(): Flow<AuthState>
}