package com.gomaa.healthy.di

import android.content.Context
import com.gomaa.healthy.data.provider.HuaweiWearProvider
import com.gomaa.healthy.data.provider.MockWearableProvider
import com.gomaa.healthy.domain.model.WearableProvider
import com.gomaa.healthy.domain.provider.WearableManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WearableModule {

    @Provides
    @Singleton
    @Named("mock")
    fun provideMockProvider(): WearableProvider {
        return MockWearableProvider()
    }

    @Provides
    @Singleton
    @Named("huawei")
    fun provideHuaweiProvider(
        @ApplicationContext context: Context
    ): WearableProvider {
        return HuaweiWearProvider(context)
    }

    @Provides
    @Singleton
    fun provideWearableManager(
        @Named("mock") mockProvider: WearableProvider,
        @Named("huawei") huaweiProvider: WearableProvider
    ): WearableManager {
        val providers = mapOf(
            "Mock" to mockProvider,
            "Huawei" to huaweiProvider
        )
        return WearableManager(providers)
    }
}