package com.gomaa.healthy.domain.usecase

import com.gomaa.healthy.domain.model.WearableProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetAvailableProvidersUseCase @Inject constructor(
    private val providers: Map<String, @JvmSuppressWildcards WearableProvider>
) {
    operator fun invoke(): List<String> = providers.keys.toList()
}

@Singleton
class GetProviderUseCase @Inject constructor(
    private val providers: Map<String, @JvmSuppressWildcards WearableProvider>
) {
    operator fun invoke(brand: String): WearableProvider? = providers[brand]
}

@Singleton
class SelectWearableProviderUseCase @Inject constructor(
    private val providers: Map<String, @JvmSuppressWildcards WearableProvider>
) {
    private val _selectedProvider = MutableStateFlow<WearableProvider?>(null)
    val selectedProvider: Flow<WearableProvider?> = _selectedProvider.asStateFlow()

    private val _selectedBrand = MutableStateFlow<String?>(null)
    val selectedBrand: Flow<String?> = _selectedBrand.asStateFlow()

    suspend operator fun invoke(brand: String): WearableProvider? {
        val provider = providers[brand]
        _selectedProvider.value = provider
        _selectedBrand.value = brand
        return provider
    }

    fun getCurrentProvider(): WearableProvider? = _selectedProvider.value

    fun getCurrentBrand(): String? = _selectedBrand.value
}

@Singleton
class ConnectWearableUseCase @Inject constructor(
    private val selectWearableProviderUseCase: SelectWearableProviderUseCase
) {
    suspend operator fun invoke(deviceId: String = "device-1"): Result<Unit> {
        return try {
            val provider = selectWearableProviderUseCase.getCurrentProvider()
                ?: return Result.failure(IllegalStateException("No provider selected"))
            provider.startMonitoring(deviceId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@Singleton
class DisconnectWearableUseCase @Inject constructor(
    private val selectWearableProviderUseCase: SelectWearableProviderUseCase
) {
    suspend operator fun invoke() {
        selectWearableProviderUseCase.getCurrentProvider()?.stopMonitoring()
    }
}

@Singleton
class HasAvailableDevicesUseCase @Inject constructor(
    private val getProviderUseCase: GetProviderUseCase
) {
    suspend operator fun invoke(brand: String): Boolean {
        return getProviderUseCase(brand)?.hasAvailableDevices() ?: false
    }
}