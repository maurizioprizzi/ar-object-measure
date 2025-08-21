package com.objectmeasure.ar.di

import android.content.Context
import com.objectmeasure.ar.BuildConfig
import com.objectmeasure.ar.data.repository.ObjectRepositoryImpl
import com.objectmeasure.ar.domain.repository.ObjectRepository
import com.objectmeasure.ar.domain.model.*
import com.objectmeasure.ar.core.util.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Módulo Hilt para configurar injeção de dependência - VERSÃO CONSOLIDADA E OTIMIZADA
 *
 * Melhorias implementadas:
 * - Resolução de conflitos de classes duplicadas
 * - Configurações flexíveis e validadas
 * - Organização modular para crescimento futuro
 * - Profiles de configuração para diferentes ambientes
 * - Validações robustas de configurações
 * - Factory patterns para configurações complexas
 * - Preparação estruturada para componentes futuros
 *
 * @version 2.2
 */

// ========== MÓDULO PRINCIPAL DE DADOS ==========

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindObjectRepository(
        objectRepositoryImpl: ObjectRepositoryImpl
    ): ObjectRepository

    // Preparação para futuras implementações
    // TODO: Adicionar quando implementar ARCore
    // @Binds @Singleton abstract fun bindARDataSource(impl: ARDataSourceImpl): ARDataSource
    // @Binds @Singleton abstract fun bindMLDataSource(impl: MLDataSourceImpl): MLDataSource
    // @Binds @Singleton abstract fun bindStorageDataSource(impl: StorageDataSourceImpl): StorageDataSource
    // @Binds @Singleton abstract fun bindNetworkDataSource(impl: NetworkDataSourceImpl): NetworkDataSource
}

// ========== MÓDULO DE CONFIGURAÇÕES ==========

@Module
@InstallIn(SingletonComponent::class)
object ConfigurationModule {

    /**
     * Configuração do cache baseada no perfil de ambiente
     */
    @Provides
    @Singleton
    fun provideCacheConfiguration(
        @ApplicationContext context: Context
    ): CacheConfiguration {
        val isDebug = BuildConfig.DEBUG
        val isLowMemoryDevice = isLowMemoryDevice(context)

        return CacheConfiguration(
            maxSize = when {
                isDebug -> 100
                isLowMemoryDevice -> 25
                else -> 50
            },
            autoCleanupEnabled = true,
            confidenceThreshold = if (isDebug) 0.5f else 0.7f,
            currentSize = 0,
            maxObjectAgeMs = if (isDebug) 10 * 60 * 1000L else 5 * 60 * 1000L, // 10min debug, 5min prod
            duplicateDetectionEnabled = true,
            cleanupIntervalMs = if (isDebug) 60_000L else 30_000L // 1min debug, 30s prod
        ).also { config ->
            debugOnly {
                config.logDebug("Cache configuration created", "DI")
            }
        }
    }

    /**
     * Configuração do ARCore otimizada por device
     */
    @Provides
    @Singleton
    fun provideARConfiguration(
        @ApplicationContext context: Context
    ): ARConfiguration {
        val deviceCapabilities = analyzeDeviceCapabilities(context)

        return ARConfiguration(
            enableCloudAnchors = !BuildConfig.DEBUG && deviceCapabilities.hasInternet,
            enableLightEstimation = deviceCapabilities.hasAdvancedCamera,
            preferredFPS = when {
                BuildConfig.DEBUG -> 15
                deviceCapabilities.isHighEndDevice -> 30
                else -> 20
            },
            autoFocus = true,
            imageStabilization = deviceCapabilities.hasImageStabilization,
            planeFindingMode = if (deviceCapabilities.isHighEndDevice) {
                PlaneFindingMode.HORIZONTAL_AND_VERTICAL
            } else {
                PlaneFindingMode.HORIZONTAL_ONLY
            },
            lightEstimationMode = if (deviceCapabilities.hasAdvancedCamera) {
                LightEstimationMode.ENVIRONMENTAL_HDR
            } else {
                LightEstimationMode.AMBIENT_INTENSITY
            },
            cameraConfig = CameraConfiguration.forDevice(deviceCapabilities)
        )
    }

    /**
     * Configuração de detecção otimizada
     */
    @Provides
    @Singleton
    fun provideDetectionConfiguration(
        @ApplicationContext context: Context
    ): DetectionConfiguration {
        val deviceCapabilities = analyzeDeviceCapabilities(context)

        return DetectionConfiguration(
            targetFPS = when {
                BuildConfig.DEBUG -> 15
                deviceCapabilities.isHighEndDevice -> 30
                else -> 20
            },
            maxObjects = if (deviceCapabilities.isHighEndDevice) 5 else 3,
            minConfidence = if (BuildConfig.DEBUG) 0.5f else 0.6f,
            useCloudAnchors = !BuildConfig.DEBUG && deviceCapabilities.hasInternet,
            enableLightEstimation = deviceCapabilities.hasAdvancedCamera,
            enableObjectTracking = true,
            trackingTimeout = 2000L,
            enableMockData = BuildConfig.DEBUG,
            processingQuality = if (deviceCapabilities.isHighEndDevice) {
                ProcessingQuality.HIGH
            } else {
                ProcessingQuality.BALANCED
            }
        )
    }

    /**
     * Configuração de medições com unidades baseadas na localização
     */
    @Provides
    @Singleton
    fun provideMeasurementConfiguration(
        @ApplicationContext context: Context
    ): MeasurementConfiguration {
        val locale = context.resources.configuration.locales[0]
        val usesMetricSystem = !setOf("US", "LR", "MM").contains(locale.country)

        return MeasurementConfiguration(
            defaultLengthUnit = if (usesMetricSystem) MeasurementUnit.CENTIMETERS else MeasurementUnit.INCHES,
            defaultWeightUnit = if (usesMetricSystem) MeasurementUnit.KILOGRAMS else MeasurementUnit.POUNDS,
            defaultVolumeUnit = if (usesMetricSystem) MeasurementUnit.LITERS else MeasurementUnit.FLUID_OUNCES,
            defaultTemperatureUnit = if (usesMetricSystem) MeasurementUnit.CELSIUS else MeasurementUnit.FAHRENHEIT,
            precision = 2,
            enableAutoUnitConversion = true,
            showMultipleUnits = BuildConfig.DEBUG,
            roundingMode = RoundingMode.HALF_UP,
            locale = locale
        )
    }

    /**
     * Configuração de performance baseada no hardware
     */
    @Provides
    @Singleton
    fun providePerformanceConfiguration(
        @ApplicationContext context: Context
    ): PerformanceConfiguration {
        val deviceCapabilities = analyzeDeviceCapabilities(context)

        return PerformanceConfiguration(
            enableParallelProcessing = deviceCapabilities.cpuCores > 4,
            maxConcurrentDetections = when {
                deviceCapabilities.cpuCores >= 8 -> 4
                deviceCapabilities.cpuCores >= 4 -> 2
                else -> 1
            },
            enableBackgroundProcessing = !deviceCapabilities.isLowMemoryDevice,
            cachePreloadSize = if (deviceCapabilities.isHighEndDevice) 20 else 10,
            enablePreviewOptimization = true,
            targetMemoryUsage = when {
                deviceCapabilities.isLowMemoryDevice -> MemoryUsage.LOW
                deviceCapabilities.availableMemoryMB > 4096 -> MemoryUsage.HIGH
                else -> MemoryUsage.MEDIUM
            }
        )
    }

    /**
     * Configuração de logging e debug
     */
    @Provides
    @Singleton
    fun provideDebugConfiguration(): DebugConfiguration {
        return DebugConfiguration(
            enableDebugLogs = BuildConfig.DEBUG,
            enablePerformanceMetrics = BuildConfig.DEBUG,
            enableCrashReporting = !BuildConfig.DEBUG,
            logLevel = if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.WARN,
            enableDetailedErrors = BuildConfig.DEBUG,
            enableMockDataGenerator = BuildConfig.DEBUG,
            saveDetectionImages = BuildConfig.DEBUG,
            enableProfiler = BuildConfig.DEBUG
        )
    }

    /**
     * Provê contexto da aplicação
     */
    @Provides
    @ApplicationContext
    fun provideApplicationContext(@ApplicationContext context: Context): Context = context
}

// ========== MÓDULO DE AMBIENTE ==========

@Module
@InstallIn(SingletonComponent::class)
abstract class EnvironmentModule {

    @Binds
    @DevelopmentRepository
    @Singleton
    abstract fun bindDevelopmentRepository(
        objectRepositoryImpl: ObjectRepositoryImpl
    ): ObjectRepository

    // TODO: Implementar quando tiver versão de produção
    // @Binds
    // @ProductionRepository
    // @Singleton
    // abstract fun bindProductionRepository(
    //     productionRepositoryImpl: ProductionObjectRepositoryImpl
    // ): ObjectRepository
}

// ========== MÓDULO DE PROFILES ==========

@Module
@InstallIn(SingletonComponent::class)
object ProfileModule {

    /**
     * Profile ativo baseado no build
     */
    @Provides
    @Singleton
    fun provideActiveProfile(): AppProfile {
        return when {
            BuildConfig.DEBUG -> AppProfile.DEVELOPMENT
            BuildConfig.BUILD_TYPE == "staging" -> AppProfile.STAGING
            else -> AppProfile.PRODUCTION
        }
    }

    /**
     * Configurações específicas do profile
     */
    @Provides
    @Singleton
    fun provideProfileConfigurations(profile: AppProfile): ProfileConfigurations {
        return when (profile) {
            AppProfile.DEVELOPMENT -> ProfileConfigurations.development()
            AppProfile.STAGING -> ProfileConfigurations.staging()
            AppProfile.PRODUCTION -> ProfileConfigurations.production()
        }
    }
}

// ========== QUALIFIERS ==========

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DevelopmentRepository

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ProductionRepository

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LocalCache

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RemoteCache

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class HighPerformance

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LowPower

// ========== ENUMS E DATA CLASSES ==========

enum class AppProfile {
    DEVELOPMENT,
    STAGING,
    PRODUCTION
}

enum class ProcessingQuality {
    LOW,
    BALANCED,
    HIGH
}

enum class MemoryUsage {
    LOW,
    MEDIUM,
    HIGH
}

enum class RoundingMode {
    HALF_UP,
    HALF_DOWN,
    CEILING,
    FLOOR
}

enum class PlaneFindingMode {
    HORIZONTAL_ONLY,
    VERTICAL_ONLY,
    HORIZONTAL_AND_VERTICAL,
    DISABLED
}

enum class LightEstimationMode {
    DISABLED,
    AMBIENT_INTENSITY,
    ENVIRONMENTAL_HDR
}

/**
 * Configuração do ARCore (renomeada para evitar conflito)
 */
data class ARConfiguration(
    val enableCloudAnchors: Boolean,
    val enableLightEstimation: Boolean,
    val preferredFPS: Int,
    val autoFocus: Boolean,
    val imageStabilization: Boolean,
    val planeFindingMode: PlaneFindingMode,
    val lightEstimationMode: LightEstimationMode,
    val cameraConfig: CameraConfiguration
) {
    init {
        require(preferredFPS in 1..60) { "FPS must be between 1 and 60" }
    }

    fun isValid(): Boolean {
        return preferredFPS > 0 && preferredFPS <= 60
    }
}

/**
 * Configuração de detecção (renomeada para evitar conflito)
 */
data class DetectionConfiguration(
    val targetFPS: Int,
    val maxObjects: Int,
    val minConfidence: Float,
    val useCloudAnchors: Boolean,
    val enableLightEstimation: Boolean,
    val enableObjectTracking: Boolean,
    val trackingTimeout: Long,
    val enableMockData: Boolean,
    val processingQuality: ProcessingQuality
) {
    init {
        require(targetFPS in 1..60) { "Target FPS must be between 1 and 60" }
        require(maxObjects in 1..10) { "Max objects must be between 1 and 10" }
        require(minConfidence in 0f..1f) { "Min confidence must be between 0.0 and 1.0" }
        require(trackingTimeout > 0) { "Tracking timeout must be positive" }
    }
}

/**
 * Configuração de medições
 */
data class MeasurementConfiguration(
    val defaultLengthUnit: MeasurementUnit,
    val defaultWeightUnit: MeasurementUnit,
    val defaultVolumeUnit: MeasurementUnit,
    val defaultTemperatureUnit: MeasurementUnit,
    val precision: Int,
    val enableAutoUnitConversion: Boolean,
    val showMultipleUnits: Boolean,
    val roundingMode: RoundingMode,
    val locale: java.util.Locale
) {
    init {
        require(precision in 0..6) { "Precision must be between 0 and 6 decimal places" }
    }
}

/**
 * Configuração de performance
 */
data class PerformanceConfiguration(
    val enableParallelProcessing: Boolean,
    val maxConcurrentDetections: Int,
    val enableBackgroundProcessing: Boolean,
    val cachePreloadSize: Int,
    val enablePreviewOptimization: Boolean,
    val targetMemoryUsage: MemoryUsage
) {
    init {
        require(maxConcurrentDetections > 0) { "Max concurrent detections must be positive" }
        require(cachePreloadSize >= 0) { "Cache preload size cannot be negative" }
    }
}

/**
 * Configuração de debug
 */
data class DebugConfiguration(
    val enableDebugLogs: Boolean,
    val enablePerformanceMetrics: Boolean,
    val enableCrashReporting: Boolean,
    val logLevel: LogLevel,
    val enableDetailedErrors: Boolean,
    val enableMockDataGenerator: Boolean,
    val saveDetectionImages: Boolean,
    val enableProfiler: Boolean
)

/**
 * Configuração da câmera
 */
data class CameraConfiguration(
    val resolution: CameraResolution,
    val enableHDR: Boolean,
    val enableImageStabilization: Boolean,
    val focusMode: FocusMode,
    val exposureMode: ExposureMode
) {
    companion object {
        fun forDevice(capabilities: DeviceCapabilities): CameraConfiguration {
            return CameraConfiguration(
                resolution = if (capabilities.isHighEndDevice) {
                    CameraResolution.HD_1080P
                } else {
                    CameraResolution.HD_720P
                },
                enableHDR = capabilities.hasAdvancedCamera,
                enableImageStabilization = capabilities.hasImageStabilization,
                focusMode = if (capabilities.hasAdvancedCamera) {
                    FocusMode.CONTINUOUS_AUTO
                } else {
                    FocusMode.AUTO
                },
                exposureMode = ExposureMode.AUTO
            )
        }
    }
}

enum class CameraResolution {
    SD_480P,
    HD_720P,
    HD_1080P,
    UHD_4K
}

enum class FocusMode {
    AUTO,
    CONTINUOUS_AUTO,
    MANUAL,
    FIXED
}

enum class ExposureMode {
    AUTO,
    MANUAL,
    SCENE_AUTO
}

/**
 * Capacidades do dispositivo
 */
data class DeviceCapabilities(
    val isHighEndDevice: Boolean,
    val isLowMemoryDevice: Boolean,
    val cpuCores: Int,
    val availableMemoryMB: Long,
    val hasAdvancedCamera: Boolean,
    val hasImageStabilization: Boolean,
    val hasInternet: Boolean,
    val supportsARCore: Boolean
)

/**
 * Configurações por profile
 */
data class ProfileConfigurations(
    val enableAnalytics: Boolean,
    val enableCrashReporting: Boolean,
    val apiEndpoint: String,
    val enableCloudSync: Boolean,
    val cacheRetentionDays: Int,
    val maxFileSize: Long
) {
    companion object {
        fun development() = ProfileConfigurations(
            enableAnalytics = false,
            enableCrashReporting = false,
            apiEndpoint = "https://dev-api.objectmeasure.com/",
            enableCloudSync = false,
            cacheRetentionDays = 1,
            maxFileSize = 50 * 1024 * 1024 // 50MB
        )

        fun staging() = ProfileConfigurations(
            enableAnalytics = true,
            enableCrashReporting = true,
            apiEndpoint = "https://staging-api.objectmeasure.com/",
            enableCloudSync = true,
            cacheRetentionDays = 7,
            maxFileSize = 25 * 1024 * 1024 // 25MB
        )

        fun production() = ProfileConfigurations(
            enableAnalytics = true,
            enableCrashReporting = true,
            apiEndpoint = "https://api.objectmeasure.com/",
            enableCloudSync = true,
            cacheRetentionDays = 30,
            maxFileSize = 10 * 1024 * 1024 // 10MB
        )
    }
}

// ========== UTILITY FUNCTIONS ==========

/**
 * Verifica se é um dispositivo com pouca memória
 */
private fun isLowMemoryDevice(context: Context): Boolean {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    return activityManager.isLowRamDevice
}

/**
 * Analisa as capacidades do dispositivo
 */
private fun analyzeDeviceCapabilities(context: Context): DeviceCapabilities {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    val memInfo = android.app.ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memInfo)

    val availableMemoryMB = memInfo.availMem / (1024 * 1024)
    val cpuCores = Runtime.getRuntime().availableProcessors()
    val isLowMemoryDevice = activityManager.isLowRamDevice

    return DeviceCapabilities(
        isHighEndDevice = !isLowMemoryDevice && availableMemoryMB > 3072 && cpuCores >= 6,
        isLowMemoryDevice = isLowMemoryDevice,
        cpuCores = cpuCores,
        availableMemoryMB = availableMemoryMB,
        hasAdvancedCamera = !isLowMemoryDevice, // Simplificado - seria verificação real
        hasImageStabilization = !isLowMemoryDevice,
        hasInternet = hasNetworkConnection(context),
        supportsARCore = !isLowMemoryDevice // Simplificado - seria verificação ARCore real
    )
}

/**
 * Verifica conexão de rede
 */
private fun hasNetworkConnection(context: Context): Boolean {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
    val activeNetwork = connectivityManager.activeNetwork ?: return false
    val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

    return networkCapabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

// ========== MÓDULOS FUTUROS (ORGANIZADOS) ==========

/**
 * Módulo para integrações AR (para implementação futura)
 */
/*
@Module
@InstallIn(SingletonComponent::class)
abstract class ARModule {

    @Binds @Singleton
    abstract fun bindARSession(impl: ARSessionImpl): ARSession

    @Binds @Singleton
    abstract fun bindARRenderer(impl: ARRendererImpl): ARRenderer

    @Binds @Singleton
    abstract fun bindPlaneDetector(impl: PlaneDetectorImpl): PlaneDetector

    @Binds @Singleton
    abstract fun bindAnchorManager(impl: AnchorManagerImpl): AnchorManager
}
*/

/**
 * Módulo para ML Kit (para implementação futura)
 */
/*
@Module
@InstallIn(SingletonComponent::class)
abstract class MLModule {

    @Binds @Singleton
    abstract fun bindObjectDetector(impl: MLKitObjectDetectorImpl): ObjectDetector

    @Binds @Singleton
    abstract fun bindImageClassifier(impl: MLKitImageClassifierImpl): ImageClassifier

    @Binds @Singleton
    abstract fun bindTextRecognizer(impl: MLKitTextRecognizerImpl): TextRecognizer
}
*/

/**
 * Módulo para persistência (para implementação futura)
 */
/*
@Module
@InstallIn(SingletonComponent::class)
abstract class StorageModule {

    @Binds @Singleton
    abstract fun bindLocalStorage(impl: RoomLocalStorageImpl): LocalStorage

    @Binds @Singleton
    abstract fun bindPreferences(impl: DataStorePreferencesImpl): UserPreferences

    @Binds @Singleton
    abstract fun bindFileManager(impl: FileManagerImpl): FileManager
}
*/