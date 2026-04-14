package com.gomaa.healthy.di

import android.content.Context
import com.gomaa.healthy.data.healthkit.HuaweiHealthKitAuthManager
import com.gomaa.healthy.data.healthkit.HuaweiHealthKitDataSource
import com.gomaa.healthy.data.healthkit.HuaweiHealthKitDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class HealthKitModule {

    @Binds
    @Singleton
    abstract fun bindHuaweiHealthKitDataSource(
        impl: HuaweiHealthKitDataSourceImpl
    ): HuaweiHealthKitDataSource

    companion object {
        @Provides
        @Singleton
        fun provideHuaweiHealthKitAuthManager(
            @ApplicationContext context: Context
        ): HuaweiHealthKitAuthManager {
            return HuaweiHealthKitAuthManager(context)
        }
    }
}