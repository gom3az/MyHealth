package com.gomaa.healthy.data.security

import com.gomaa.healthy.data.healthkit.AuthState
import com.gomaa.healthy.data.healthkit.AuthTokens
import com.gomaa.healthy.di.TokenStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/**
 * Encrypted token storage using DataStore + Tink encryption.
 * Replaces deprecated EncryptedSharedPreferences with modern encryption.
 */
class EncryptedTokenStorage @Inject constructor(
    private val encryptedPrefsManager: EncryptedPreferencesManager
) : TokenStorage {

    override suspend fun saveTokens(tokens: AuthTokens) {
        encryptedPrefsManager.saveEncryptedString(
            EncryptedPreferencesManager.KEY_ACCESS_TOKEN,
            tokens.accessToken
        )
        encryptedPrefsManager.saveEncryptedString(
            EncryptedPreferencesManager.KEY_REFRESH_TOKEN,
            tokens.refreshToken
        )
        encryptedPrefsManager.saveEncryptedLong(
            EncryptedPreferencesManager.KEY_TOKEN_EXPIRY,
            tokens.tokenExpiry
        )
        encryptedPrefsManager.saveEncryptedString(
            EncryptedPreferencesManager.KEY_USER_ID,
            tokens.userId
        )
        encryptedPrefsManager.saveEncryptedString(
            EncryptedPreferencesManager.KEY_SCOPES,
            tokens.scopes
        )
    }

    override suspend fun loadTokens(): AuthTokens? {
        val accessToken = encryptedPrefsManager.getEncryptedString(
            EncryptedPreferencesManager.KEY_ACCESS_TOKEN
        )
        val refreshToken = encryptedPrefsManager.getEncryptedString(
            EncryptedPreferencesManager.KEY_REFRESH_TOKEN
        )

        if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) {
            return null
        }

        val tokenExpiry = encryptedPrefsManager.getEncryptedLong(
            EncryptedPreferencesManager.KEY_TOKEN_EXPIRY
        )
        val userId = encryptedPrefsManager.getEncryptedString(
            EncryptedPreferencesManager.KEY_USER_ID
        ) ?: ""
        val scopes = encryptedPrefsManager.getEncryptedString(
            EncryptedPreferencesManager.KEY_SCOPES
        ) ?: ""

        return AuthTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenExpiry = tokenExpiry,
            userId = userId,
            scopes = scopes
        )
    }

    override suspend fun clearTokens() {
        encryptedPrefsManager.removeEncryptedString(EncryptedPreferencesManager.KEY_ACCESS_TOKEN)
        encryptedPrefsManager.removeEncryptedString(EncryptedPreferencesManager.KEY_REFRESH_TOKEN)
        encryptedPrefsManager.removeEncryptedString(EncryptedPreferencesManager.KEY_TOKEN_EXPIRY)
        encryptedPrefsManager.removeEncryptedString(EncryptedPreferencesManager.KEY_USER_ID)
        encryptedPrefsManager.removeEncryptedString(EncryptedPreferencesManager.KEY_SCOPES)
    }

    override suspend fun getAccessToken(): String? {
        return encryptedPrefsManager.getEncryptedString(EncryptedPreferencesManager.KEY_ACCESS_TOKEN)
    }

    override suspend fun getTokenExpiry(): Long {
        return encryptedPrefsManager.getEncryptedLong(EncryptedPreferencesManager.KEY_TOKEN_EXPIRY)
    }

    override suspend fun getRefreshToken(): String? {
        return encryptedPrefsManager.getEncryptedString(EncryptedPreferencesManager.KEY_REFRESH_TOKEN)
    }

    override fun authStateFlow(): Flow<AuthState> {
        return combine(
            encryptedPrefsManager.observeEncryptedString(EncryptedPreferencesManager.KEY_ACCESS_TOKEN),
            encryptedPrefsManager.observeEncryptedLong(EncryptedPreferencesManager.KEY_TOKEN_EXPIRY)
        ) { accessToken, tokenExpiry ->
            computeAuthState(accessToken, tokenExpiry)
        }
    }

    private fun computeAuthState(accessToken: String?, tokenExpiry: Long): AuthState {
        return when {
            accessToken.isNullOrBlank() -> AuthState.NOT_SIGNED_IN
            System.currentTimeMillis() >= tokenExpiry -> AuthState.TOKEN_EXPIRED
            else -> AuthState.SIGNED_IN
        }
    }
}