package com.gomaa.healthy.di

import android.content.Context
import androidx.security.crypto.MasterKey
import com.gomaa.healthy.data.security.EncryptedTokenStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideMasterKey(@ApplicationContext context: Context): MasterKey {
        return MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    }

    @Provides
    @Singleton
    fun provideTokenStorage(
        @ApplicationContext context: Context, masterKey: MasterKey
    ): TokenStorage {
        return EncryptedTokenStorage(context, masterKey)
    }
}