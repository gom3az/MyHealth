package com.gomaa.healthy.di

import com.gomaa.healthy.data.security.EncryptedTokenStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun provideTokenStorage(
        encryptedTokenStorage: EncryptedTokenStorage
    ): TokenStorage {
        return encryptedTokenStorage
    }
}