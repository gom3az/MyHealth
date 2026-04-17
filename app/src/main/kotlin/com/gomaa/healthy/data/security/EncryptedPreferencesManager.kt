package com.gomaa.healthy.data.security

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private const val KEYSET_NAME = "encrypted_prefs_keyset"
private const val PREF_FILE_NAME = "encrypted_prefs_keyset_prefs"
private const val MASTER_KEY_URI = "android-keystore://encrypted_prefs_master_key"
private const val DATASTORE_NAME = "encrypted_preferences"

/**
 * EncryptedPreferencesManager provides secure storage using Google Tink encryption
 * with Android Keystore for key management and DataStore for persistence.
 *
 * This replaces the deprecated EncryptedSharedPreferences with a modern solution.
 */
@Singleton
class EncryptedPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object {
        // Token storage keys
        const val KEY_ACCESS_TOKEN = "huawei_access_token"
        const val KEY_REFRESH_TOKEN = "huawei_refresh_token"
        const val KEY_TOKEN_EXPIRY = "huawei_token_expiry"
        const val KEY_USER_ID = "huawei_user_id"
        const val KEY_SCOPES = "huawei_scopes"

        // Health Connect sync keys
        const val KEY_LAST_SYNC_TIME = "health_connect_last_sync"
        const val KEY_SYNC_ENABLED = "health_connect_sync_enabled"
    }

    private val aead: Aead by lazy {
        // Initialize Tink Aead
        AeadConfig.register()

        val keysetHandle = AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREF_FILE_NAME)
            .withKeyTemplate(com.google.crypto.tink.KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle

        // Get the Aead primitive directly from KeysetHandle
        keysetHandle.getPrimitive(Aead::class.java)
    }

    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(DATASTORE_NAME)

    // String operations

    suspend fun saveEncryptedString(key: String, value: String) {
        val encrypted = aead.encrypt(value.toByteArray(Charsets.UTF_8), null)
        val encoded = android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP)
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey(key)] = encoded
        }
    }

    suspend fun getEncryptedString(key: String): String? {
        val encoded = context.dataStore.data.first()[stringPreferencesKey(key)] ?: return null
        val encrypted = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)
        val decrypted = aead.decrypt(encrypted, null)
        return String(decrypted, Charsets.UTF_8)
    }

    suspend fun removeEncryptedString(key: String) {
        context.dataStore.edit { prefs ->
            prefs.remove(stringPreferencesKey(key))
        }
    }

    // Long operations

    suspend fun saveEncryptedLong(key: String, value: Long) {
        val encrypted = aead.encrypt(value.toString().toByteArray(Charsets.UTF_8), null)
        val encoded = android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP)
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey(key)] = encoded
        }
    }

    suspend fun getEncryptedLong(key: String, defaultValue: Long = 0L): Long {
        val encoded =
            context.dataStore.data.first()[stringPreferencesKey(key)] ?: return defaultValue
        val encrypted = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)
        val decrypted = aead.decrypt(encrypted, null)
        return String(decrypted, Charsets.UTF_8).toLongOrNull() ?: defaultValue
    }

    // Boolean operations (stored as string)

    suspend fun saveEncryptedBoolean(key: String, value: Boolean) {
        val encrypted = aead.encrypt(value.toString().toByteArray(Charsets.UTF_8), null)
        val encoded = android.util.Base64.encodeToString(encrypted, android.util.Base64.NO_WRAP)
        context.dataStore.edit { prefs ->
            prefs[stringPreferencesKey(key)] = encoded
        }
    }

    suspend fun getEncryptedBoolean(key: String, defaultValue: Boolean = false): Boolean {
        val encoded =
            context.dataStore.data.first()[stringPreferencesKey(key)] ?: return defaultValue
        val encrypted = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)
        val decrypted = aead.decrypt(encrypted, null)
        return String(decrypted, Charsets.UTF_8).toBooleanStrictOrNull() ?: defaultValue
    }

    // Flow-based watching for specific keys

    fun observeEncryptedString(key: String): Flow<String?> {
        return context.dataStore.data.map { prefs ->
            val encoded = prefs[stringPreferencesKey(key)] ?: return@map null
            try {
                val encrypted = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)
                val decrypted = aead.decrypt(encrypted, null)
                String(decrypted, Charsets.UTF_8)
            } catch (e: Exception) {
                null
            }
        }
    }

    fun observeEncryptedLong(key: String): Flow<Long> {
        return context.dataStore.data.map { prefs ->
            val encoded = prefs[stringPreferencesKey(key)] ?: return@map 0L
            try {
                val encrypted = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)
                val decrypted = aead.decrypt(encrypted, null)
                String(decrypted, Charsets.UTF_8).toLongOrNull() ?: 0L
            } catch (e: Exception) {
                0L
            }
        }
    }

    fun observeEncryptedBoolean(key: String): Flow<Boolean> {
        return context.dataStore.data.map { prefs ->
            val encoded = prefs[stringPreferencesKey(key)] ?: return@map false
            try {
                val encrypted = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)
                val decrypted = aead.decrypt(encrypted, null)
                String(decrypted, Charsets.UTF_8).toBooleanStrictOrNull() ?: false
            } catch (e: Exception) {
                false
            }
        }
    }

    // Clear all encrypted data

    suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}