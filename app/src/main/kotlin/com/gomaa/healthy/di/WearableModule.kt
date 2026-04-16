package com.gomaa.healthy.di

import android.content.Context
import com.gomaa.healthy.data.provider.HuaweiDeviceDiscoverer
import com.gomaa.healthy.data.provider.HuaweiHeartRateMonitor
import com.gomaa.healthy.data.provider.HuaweiWearProvider
import com.gomaa.healthy.data.provider.MockWearableProvider
import com.gomaa.healthy.domain.model.WearableProvider
import com.gomaa.healthy.logging.AppLogger
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
        @ApplicationContext context: Context,
        deviceDiscoverer: HuaweiDeviceDiscoverer,
        heartRateMonitor: HuaweiHeartRateMonitor,
        appLogger: AppLogger
    ): WearableProvider {
        return HuaweiWearProvider(context, deviceDiscoverer, heartRateMonitor, appLogger)
    }

    @Provides
    @Singleton
    fun provideProvidersMap(
        @Named("mock") mockProvider: WearableProvider,
        @Named("huawei") huaweiProvider: WearableProvider
    ): Map<String, WearableProvider> {
        return mapOf(
            "Mock" to mockProvider,
            "Huawei" to huaweiProvider
        )
    }
}