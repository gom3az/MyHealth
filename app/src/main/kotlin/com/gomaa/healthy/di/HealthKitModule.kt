package com.gomaa.healthy.di

import com.gomaa.healthy.data.healthkit.HuaweiHealthKitDataSource
import com.gomaa.healthy.data.healthkit.HuaweiHealthKitDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
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
}