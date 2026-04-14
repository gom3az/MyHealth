package com.gomaa.healthy.data.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.gomaa.healthy.data.healthkit.AuthState
import com.gomaa.healthy.data.healthkit.AuthTokens
import com.gomaa.healthy.di.TokenStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

private const val PREFS_NAME = "huawei_healthkit_auth"

private const val KEY_ACCESS_TOKEN = "huawei_access_token"
private const val KEY_REFRESH_TOKEN = "huawei_refresh_token"
private const val KEY_TOKEN_EXPIRY = "huawei_token_expiry"
private const val KEY_USER_ID = "huawei_user_id"
private const val KEY_SCOPES = "huawei_scopes"

class EncryptedTokenStorage(
    context: Context, masterKey: MasterKey
) : TokenStorage {

    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun saveTokens(tokens: AuthTokens) {
        encryptedPrefs.edit().putString(KEY_ACCESS_TOKEN, tokens.accessToken)
            .putString(KEY_REFRESH_TOKEN, tokens.refreshToken)
            .putLong(KEY_TOKEN_EXPIRY, tokens.tokenExpiry).putString(KEY_USER_ID, tokens.userId)
            .putString(KEY_SCOPES, tokens.scopes).apply()
    }

    override fun loadTokens(): AuthTokens? {
        val accessToken = encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)
        val refreshToken = encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)
        val tokenExpiry = encryptedPrefs.getLong(KEY_TOKEN_EXPIRY, 0L)
        val userId = encryptedPrefs.getString(KEY_USER_ID, null)
        val scopes = encryptedPrefs.getString(KEY_SCOPES, null)

        if (accessToken.isNullOrBlank() || refreshToken.isNullOrBlank()) {
            return null
        }

        return AuthTokens(
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenExpiry = tokenExpiry,
            userId = userId ?: "",
            scopes = scopes ?: ""
        )
    }

    override fun clearTokens() {
        val success = encryptedPrefs.edit().remove(KEY_ACCESS_TOKEN).remove(KEY_REFRESH_TOKEN)
            .remove(KEY_TOKEN_EXPIRY).remove(KEY_USER_ID).remove(KEY_SCOPES).commit()
        if (!success) {
            android.util.Log.w("EncryptedTokenStorage", "Failed to clear tokens")
        }
    }

    override fun getAccessToken(): String? {
        return encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)
    }

    override fun getTokenExpiry(): Long {
        return encryptedPrefs.getLong(KEY_TOKEN_EXPIRY, 0L)
    }

    override fun getRefreshToken(): String? {
        return encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)
    }

    override fun authStateFlow(): Flow<AuthState> = callbackFlow {
        val listener =
            android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
                if (key == KEY_ACCESS_TOKEN || key == KEY_TOKEN_EXPIRY) {
                    trySend(computeAuthState())
                }
            }

        // Emit initial state
        trySend(computeAuthState())

        encryptedPrefs.registerOnSharedPreferenceChangeListener(listener)

        awaitClose {
            encryptedPrefs.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    private fun computeAuthState(): AuthState {
        val accessToken = encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)
        val tokenExpiry = encryptedPrefs.getLong(KEY_TOKEN_EXPIRY, 0L)

        return when {
            accessToken.isNullOrBlank() -> AuthState.NOT_SIGNED_IN
            System.currentTimeMillis() >= tokenExpiry -> AuthState.TOKEN_EXPIRED
            else -> AuthState.SIGNED_IN
        }
    }
}