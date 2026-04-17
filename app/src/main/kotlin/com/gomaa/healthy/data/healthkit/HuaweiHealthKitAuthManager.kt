package com.gomaa.healthy.data.healthkit

import com.gomaa.healthy.BuildConfig
import com.gomaa.healthy.di.TokenStorage
import com.gomaa.healthy.logging.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
        data object NeedsReauth : Error("User must re-authenticate")
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
 * Uses TokenStorage (EncryptedSharedPreferences) for secure token storage.
 * Uses Account Kit to obtain OAuth tokens for Health Kit API access.
 * 
 * Note: Requires Account Kit to be enabled in AppGallery Connect console.
 */
@Singleton
class HuaweiHealthKitAuthManager @Inject constructor(
    private val tokenStorage: TokenStorage,
    private val appLogger: AppLogger
) {
    companion object {
        private const val TAG = "HealthKitAuthManager"
        private val TOKEN_REFRESH_THRESHOLD_MS = TimeUnit.DAYS.toMillis(1)
    }

    /**
     * Flow of authentication state. Emits whenever auth state changes.
     */
    val authState: Flow<AuthState> = tokenStorage.authStateFlow()

    suspend fun signIn(): HealthKitAuthResult {
        return withContext(Dispatchers.IO) {
            try {
                if (BuildConfig.DEBUG) {
                    appLogger.d(TAG, "signIn: Initiating Huawei ID sign-in")
                }

                // TODO: Implement actual Huawei Account Kit Sign-In
                // The proper implementation requires AccountAuthService from HMS Account Kit SDK:
                //
                // val authParam = AccountAuthParams.Builder()
                //     .requestIdToken()
                //     .requestUserInfo()
                //     .setScope(HealthKitScopes.allScopesString)
                //     .createAccountAuthParams()
                // val authService = AccountAuth.getService(context, authParam)
                // val signInResult = authService.silentSignIn()
                // if (signInResult.success) {
                //     val authResult = signInResult.data
                //     tokenStorage.saveTokens(
                //         AuthTokens(
                //             accessToken = authResult.accessToken,
                //             refreshToken = authResult.refreshToken,
                //             tokenExpiry = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1),
                //             userId = authResult.accountUid,
                //             scopes = HealthKitScopes.allScopesString
                //         )
                //     )
                //     return@withContext HealthKitAuthResult.Success(authResult.idToken)
                // }
                //
                // If silent sign-in fails, use:
                // val signInIntent = authService.signInIntent
                // // Launch intent for interactive sign-in

                if (BuildConfig.DEBUG) {
                    appLogger.d(TAG, "signIn: No stored tokens - user must sign in via Account Kit")
                }

                // Return NotSignedIn - real sign-in requires HMS Account Kit integration
                // This ensures proper auth flow instead of mock tokens in both debug and release
                return@withContext HealthKitAuthResult.Error.NotSignedIn
            } catch (e: Exception) {
                appLogger.e(TAG, "signIn: Error", e)
                HealthKitAuthResult.Error.NetworkError(e)
            }
        }
    }

    suspend fun refreshToken(): HealthKitAuthResult {
        return withContext(Dispatchers.IO) {
            try {
                if (BuildConfig.DEBUG) {
                    appLogger.d(TAG, "refreshToken: Refreshing OAuth token")
                }

                val tokens = tokenStorage.loadTokens()
                val refreshToken = tokenStorage.getRefreshToken()
                if (tokens == null || refreshToken.isNullOrEmpty()) {
                    if (BuildConfig.DEBUG) {
                        appLogger.d(TAG, "refreshToken: No refresh token available")
                    }
                    return@withContext HealthKitAuthResult.Error.NotSignedIn
                }

                // TODO: Implement actual token refresh using Huawei Account Kit
                // The proper implementation using HMS Account Kit SDK:
                //
                // val authService = AccountAuth.getService(context, authParams)
                // val refreshResult = authService.refreshToken(refreshToken)
                // if (refreshResult.success) {
                //     val newAuthResult = refreshResult.data
                //     tokenStorage.saveTokens(
                //         tokens.copy(
                //             accessToken = newAuthResult.accessToken,
                //             refreshToken = newAuthResult.refreshToken ?: refreshToken,
                //             tokenExpiry = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1)
                //         )
                //     )
                //     return@withContext HealthKitAuthResult.Success(newAuthResult.accessToken)
                // }
                //
                // If refresh fails with TOKEN_EXPIRED, user must re-authenticate:
                // return@withContext HealthKitAuthResult.Error.NeedsReauth

                // For now, check if we have a valid stored refresh token
                // If no valid refresh token exists, user must sign in again
                if (tokens.tokenExpiry <= System.currentTimeMillis()) {
                    // Token has expired and we don't have valid refresh capability
                    // Clear tokens and require re-authentication
                    if (BuildConfig.DEBUG) {
                        appLogger.d(TAG, "refreshToken: Token expired, clearing tokens")
                    }
                    tokenStorage.clearTokens()
                    return@withContext HealthKitAuthResult.Error.NeedsReauth
                }

                // Token still valid - return it
                if (BuildConfig.DEBUG) {
                    appLogger.d(TAG, "refreshToken: Current token still valid")
                }
                HealthKitAuthResult.Success(tokens.accessToken)
            } catch (e: Exception) {
                appLogger.e(TAG, "refreshToken: Error", e)
                HealthKitAuthResult.Error.NetworkError(e)
            }
        }
    }

    suspend fun getValidToken(): HealthKitAuthResult {
        return withContext(Dispatchers.IO) {
            try {
                val tokens = tokenStorage.loadTokens()
                if (tokens == null) {
                    return@withContext HealthKitAuthResult.Error.NotSignedIn
                }

                if (System.currentTimeMillis() >= tokens.tokenExpiry - TOKEN_REFRESH_THRESHOLD_MS) {
                    if (BuildConfig.DEBUG) {
                        appLogger.d(
                            TAG,
                            "getValidToken: Token expired or expiring soon, refreshing"
                        )
                    }
                    return@withContext refreshToken()
                }

                HealthKitAuthResult.Success(tokens.accessToken)
            } catch (e: Exception) {
                appLogger.e(TAG, "getValidToken: Error", e)
                HealthKitAuthResult.Error.NetworkError(e)
            }
        }
    }

    suspend fun signOut(): HealthKitAuthResult {
        return withContext(Dispatchers.IO) {
            try {
                if (BuildConfig.DEBUG) {
                    appLogger.d(TAG, "signOut: Clearing tokens")
                }
                tokenStorage.clearTokens()

                // TODO: Revoke token on server
                // Using Account Kit:
                // val authService = AccountAuth.getService(context, ...)
                // authService.cancelAuthorization()

                if (BuildConfig.DEBUG) {
                    appLogger.d(TAG, "signOut: Signed out successfully")
                }
                HealthKitAuthResult.Success("")
            } catch (e: Exception) {
                appLogger.e(TAG, "signOut: Error", e)
                HealthKitAuthResult.Error.NetworkError(e)
            }
        }
    }

    suspend fun isSignedIn(): Boolean {
        return tokenStorage.loadTokens() != null
    }

    suspend fun hasRequiredScopes(): Boolean {
        return try {
            val tokens = tokenStorage.loadTokens() ?: return false
            HealthKitScopes.allScopes.all { scope -> tokens.scopes.contains(scope) }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getUserId(): String? {
        return tokenStorage.loadTokens()?.userId
    }

    suspend fun getTokenExpiry(): Long {
        return tokenStorage.getTokenExpiry()
    }
}