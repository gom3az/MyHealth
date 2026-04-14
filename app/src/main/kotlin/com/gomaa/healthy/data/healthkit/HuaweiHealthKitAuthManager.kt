package com.gomaa.healthy.data.healthkit

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Required OAuth scopes for Health Kit access.
 */
object HealthKitScopes {
    const val HEALTHKIT_STEP_READ = "https://www.huawei.com/healthkit/step.read"
    const val HEALTHKIT_HEARTRATE_READ = "https://www.huawei.com/healthkit.heartrate.read"
    const val HEALTHKIT_WORKOUT_READ = "https://www.huawei.com/healthkit.workout.read"

    val allScopes = listOf(
        HEALTHKIT_STEP_READ,
        HEALTHKIT_HEARTRATE_READ,
        HEALTHKIT_WORKOUT_READ
    )

    val allScopesString = allScopes.joinToString(" ")
}

/**
 * Auth result wrapper for OAuth operations.
 */
sealed class HealthKitAuthResult {
    data class Success(val token: String, val userId: String? = null) : HealthKitAuthResult()
    sealed class Error(val message: String) : HealthKitAuthResult() {
        data object NotSignedIn : Error("User not signed in")
        data object TokenExpired : Error("OAuth token expired")
        data object PermissionDenied : Error("OAuth permission denied")
        data class NetworkError(val exception: Exception) : Error("Network error")
    }
}

/**
 * Authentication state for Huawei ID Sign-In.
 */
enum class AuthState {
    NOT_SIGNED_IN,
    SIGNED_IN,
    TOKEN_EXPIRED,
    NEEDS_REAUTH
}

/**
 * Huawei Health Kit Authentication Manager using Account Kit OAuth 2.0.
 * 
 * Uses EncryptedSharedPreferences for secure token storage.
 * Uses Account Kit to obtain OAuth tokens for Health Kit API access.
 * 
 * Note: Requires Account Kit to be enabled in AppGallery Connect console.
 */
@Singleton
class HuaweiHealthKitAuthManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "HealthKitAuthManager"

        // EncryptedSharedPreferences file name
        private const val PREFS_FILE_NAME = "huawei_healthkit_auth"

        // Preference keys for token storage
        private const val KEY_ACCESS_TOKEN = "huawei_access_token"
        private const val KEY_REFRESH_TOKEN = "huawei_refresh_token"
        private const val KEY_TOKEN_EXPIRY = "huawei_token_expiry"
        private const val KEY_USER_ID = "huawei_user_id"
        private const val KEY_SCOPES = "huawei_scopes"

        // Token refresh threshold (refresh 1 day before expiry)
        private val TOKEN_REFRESH_THRESHOLD_MS = TimeUnit.DAYS.toMillis(1)
    }

    private val masterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val encryptedPrefs by lazy {
        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // State flow for UI observation
    private val _authState = MutableStateFlow(AuthState.NOT_SIGNED_IN)
    val authState: Flow<AuthState> = _authState.asStateFlow()

    init {
        // Initialize auth state on creation
        updateAuthState()
    }

    private fun updateAuthState() {
        val token = encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)
        val expiry = encryptedPrefs.getLong(KEY_TOKEN_EXPIRY, 0L)

        _authState.value = when {
            token.isNullOrEmpty() -> AuthState.NOT_SIGNED_IN
            System.currentTimeMillis() >= expiry -> AuthState.TOKEN_EXPIRED
            else -> AuthState.SIGNED_IN
        }
    }

    /**
     * Triggers Huawei ID Sign-In flow.
     * 
     * Note: This uses Account Kit's auth API. In production:
     * - Use HmsInstanceId for token request
     * - Request oauth长城华为授权服务
     * - Handle the auth result
     */
    suspend fun signIn(): HealthKitAuthResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "signIn: Initiating Huawei ID sign-in")

                // TODO: Implement actual Huawei ID Sign-In
                // Using Account Kit:
                // val authParam = AccountAuthParams.Builder()
                //     .requestIdToken()  // Request ID token
                //     .requestUserInfo()  // Request user info
                //     .setScope(HealthKitScopes.allScopesString)
                //     .createAccountAuthParams()
                // val authService = AccountAuth.getService(context, authParam)
                // val signInResult = authService.silentSignIn()
                // if (signInResult.success) {
                //     val token = signInResult.data.idToken
                //     saveTokens(token, ...)
                //     return HealthKitAuthResult.Success(token)
                // } else {
                //     // Need interactive sign-in
                //     val intent = authService.pendingIntent
                //     // Launch intent for user interaction
                // }

                // Placeholder - returns signed in state for now
                // In production, this would launch Huawei ID sign-in activity
                Log.i(TAG, "signIn: Sign-in flow would be launched here")

                // For development, mock the token storage
                saveTokens(
                    accessToken = "mock_access_token_${System.currentTimeMillis()}",
                    refreshToken = "mock_refresh_token",
                    tokenExpiry = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30),
                    userId = "mock_user_${System.currentTimeMillis()}",
                    scopes = HealthKitScopes.allScopesString
                )

                updateAuthState()
                HealthKitAuthResult.Success("mock_access_token")
            } catch (e: Exception) {
                Log.e(TAG, "signIn: Error", e)
                HealthKitAuthResult.Error.NetworkError(e)
            }
        }
    }

    /**
     * Refreshes the OAuth token using the refresh token.
     */
    suspend fun refreshToken(): HealthKitAuthResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "refreshToken: Refreshing OAuth token")

                // Get refresh token from encrypted storage
                val refreshToken = encryptedPrefs.getString(KEY_REFRESH_TOKEN, null)
                if (refreshToken.isNullOrEmpty()) {
                    Log.w(TAG, "refreshToken: No refresh token available")
                    updateAuthState()
                    return@withContext HealthKitAuthResult.Error.NotSignedIn
                }

                // TODO: Implement actual token refresh
                // Using HmsInstanceId:
                // val instanceId = HmsInstanceId.getInstance(context)
                // val token = instanceId.getToken("oauth长城华为授权服务", "HEALTH_KIT", null)
                // or use Account Kit refresh API

                // For now, create new mock token
                val newExpiry = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(30)
                val newAccessToken = "refreshed_token_${System.currentTimeMillis()}"

                // Update token in encrypted storage
                encryptedPrefs.edit()
                    .putString(KEY_ACCESS_TOKEN, newAccessToken)
                    .putLong(KEY_TOKEN_EXPIRY, newExpiry)
                    .apply()

                Log.i(TAG, "refreshToken: Token refreshed successfully")
                updateAuthState()
                HealthKitAuthResult.Success(newAccessToken)
            } catch (e: Exception) {
                Log.e(TAG, "refreshToken: Error", e)
                HealthKitAuthResult.Error.NetworkError(e)
            }
        }
    }

    /**
     * Gets a valid OAuth token, refreshing if necessary.
     */
    suspend fun getValidToken(): HealthKitAuthResult {
        return withContext(Dispatchers.IO) {
            try {
                // Check if we have a token
                val currentToken = encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)
                if (currentToken.isNullOrEmpty()) {
                    updateAuthState()
                    return@withContext HealthKitAuthResult.Error.NotSignedIn
                }

                // Check if token is expired or about to expire
                val tokenExpiry = encryptedPrefs.getLong(KEY_TOKEN_EXPIRY, 0L)
                if (System.currentTimeMillis() >= tokenExpiry - TOKEN_REFRESH_THRESHOLD_MS) {
                    Log.i(TAG, "getValidToken: Token expired or expiring soon, refreshing")
                    val refreshResult = refreshToken()
                    if (refreshResult is HealthKitAuthResult.Success) {
                        return@withContext refreshResult
                    } else {
                        updateAuthState()
                        return@withContext refreshResult
                    }
                }

                HealthKitAuthResult.Success(currentToken)
            } catch (e: Exception) {
                Log.e(TAG, "getValidToken: Error", e)
                HealthKitAuthResult.Error.NetworkError(e)
            }
        }
    }

    /**
     * Revokes and clears all OAuth tokens.
     */
    suspend fun signOut(): HealthKitAuthResult {
        return withContext(Dispatchers.IO) {
            try {
                Log.i(TAG, "signOut: Clearing tokens")

                encryptedPrefs.edit()
                    .remove(KEY_ACCESS_TOKEN)
                    .remove(KEY_REFRESH_TOKEN)
                    .remove(KEY_TOKEN_EXPIRY)
                    .remove(KEY_USER_ID)
                    .remove(KEY_SCOPES)
                    .apply()

                // TODO: Revoke token on server
                // Using Account Kit:
                // val authService = AccountAuth.getService(context, ...)
                // authService.cancelAuthorization()

                Log.i(TAG, "signOut: Signed out successfully")
                updateAuthState()
                HealthKitAuthResult.Success("")
            } catch (e: Exception) {
                Log.e(TAG, "signOut: Error", e)
                HealthKitAuthResult.Error.NetworkError(e)
            }
        }
    }

    /**
     * Checks if user is signed in.
     */
    suspend fun isSignedIn(): Boolean {
        return try {
            val token = encryptedPrefs.getString(KEY_ACCESS_TOKEN, null)
            !token.isNullOrEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if the required scopes are authorized.
     */
    suspend fun hasRequiredScopes(): Boolean {
        return try {
            val scopes = encryptedPrefs.getString(KEY_SCOPES, null) ?: ""
            // Check if all required scopes are present
            HealthKitScopes.allScopes.all { scope -> scopes.contains(scope) }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Gets the user ID if signed in.
     */
    suspend fun getUserId(): String? {
        return encryptedPrefs.getString(KEY_USER_ID, null)
    }

    /**
     * Gets the token expiry timestamp.
     */
    suspend fun getTokenExpiry(): Long {
        return encryptedPrefs.getLong(KEY_TOKEN_EXPIRY, 0L)
    }

    private fun saveTokens(
        accessToken: String,
        refreshToken: String,
        tokenExpiry: Long,
        userId: String,
        scopes: String
    ) {
        encryptedPrefs.edit()
            .putString(KEY_ACCESS_TOKEN, accessToken)
            .putString(KEY_REFRESH_TOKEN, refreshToken)
            .putLong(KEY_TOKEN_EXPIRY, tokenExpiry)
            .putString(KEY_USER_ID, userId)
            .putString(KEY_SCOPES, scopes)
            .apply()
    }
}
