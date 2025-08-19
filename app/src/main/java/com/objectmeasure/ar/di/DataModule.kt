package com.objectmeasure.ar.di

import android.content.Context
import com.objectmeasure.ar.data.repository.ObjectRepositoryImpl
import com.objectmeasure.ar.domain.repository.ObjectRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton

/**
 * Módulo Hilt para configurar injeção de dependência - DIA 3 REVISADO
 * Organizado para crescimento futuro com ARCore e ML Kit
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    // ========== REPOSITORY BINDINGS ==========

    /**
     * Conecta interface ObjectRepository com implementação ObjectRepositoryImpl
     */
    @Binds
    @Singleton
    abstract fun bindObjectRepository(
        objectRepositoryImpl: ObjectRepositoryImpl
    ): ObjectRepository

    // ========== FUTURAS IMPLEMENTAÇÕES ==========

    // DIA 5: ARCore DataSource
    // @Binds
    // @Singleton
    // abstract fun bindARDataSource(
    //     arDataSourceImpl: ARDataSourceImpl
    // ): ARDataSource

    // DIA 6: ML Kit DataSource
    // @Binds
    // @Singleton
    // abstract fun bindMLDataSource(
    //     mlDataSourceImpl: MLDataSourceImpl
    // ): MLDataSource

    // DIA 7: Storage DataSource
    // @Binds
    // @Singleton
    // abstract fun bindStorageDataSource(
    //     storageDataSourceImpl: StorageDataSourceImpl
    // ): StorageDataSource

    // DIA 8: Network DataSource (para sync)
    // @Binds
    // @Singleton
    // abstract fun bindNetworkDataSource(
    //     networkDataSourceImpl: NetworkDataSourceImpl
    // ): NetworkDataSource
}

/**
 * Módulo para configurações e objetos que precisam de @Provides
 */
@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {

    // ========== CONFIGURAÇÕES DE CACHE ==========

    /**
     * Configuração do cache baseada no ambiente
     */
    @Provides
    @Singleton
    fun provideCacheConfig(): CacheConfig {
        return CacheConfig(
            maxSize = if (BuildConfig.DEBUG) 100 else 50,
            enableDebugLogs = BuildConfig.DEBUG,
            autoCleanupEnabled = true,
            confidenceThreshold = 0.7f,
            maxObjectAge = 5 * 60 * 1000L // 5 minutos
        )
    }

    // ========== CONFIGURAÇÕES DE AR ==========

    /**
     * Configuração padrão do ARCore
     */
    @Provides
    @Singleton
    fun provideARConfig(): ARConfig {
        return ARConfig(
            enableCloudAnchors = !BuildConfig.DEBUG, // Apenas em produção
            enableLightEstimation = true,
            preferredFPS = if (BuildConfig.DEBUG) 15 else 30, // Menor FPS em debug
            autoFocus = true,
            imageStabilization = true
        )
    }

    // ========== CONFIGURAÇÕES DE DETECÇÃO ==========

    /**
     * Configuração de detecção de objetos
     */
    @Provides
    @Singleton
    fun provideDetectionConfig(): DetectionConfig {
        return DetectionConfig(
            minConfidenceThreshold = 0.6f,
            maxObjectsPerFrame = 5,
            enableObjectTracking = true,
            trackingTimeout = 2000L, // 2 segundos
            enableMockData = BuildConfig.DEBUG
        )
    }

    // ========== CONFIGURAÇÕES DE MEDIÇÃO ==========

    /**
     * Configuração de medições
     */
    @Provides
    @Singleton
    fun provideMeasurementConfig(): MeasurementConfig {
        return MeasurementConfig(
            defaultLengthUnit = MeasurementUnit.CENTIMETERS,
            defaultWeightUnit = MeasurementUnit.KILOGRAMS,
            defaultVolumeUnit = MeasurementUnit.LITERS,
            precision = 2,
            enableAutoUnitConversion = true
        )
    }

    // ========== CONTEXTO DA APLICAÇÃO ==========

    /**
     * Fornece contexto da aplicação para componentes que precisam
     */
    @Provides
    @ApplicationContext
    fun provideApplicationContext(@ApplicationContext context: Context): Context = context
}

/**
 * Módulo para diferentes implementações baseadas em ambiente
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class EnvironmentModule {

    // ========== IMPLEMENTAÇÕES POR AMBIENTE ==========

    /**
     * Repository para desenvolvimento (com mock data)
     */
    @Binds
    @DevelopmentRepository
    @Singleton
    abstract fun bindDevelopmentRepository(
        objectRepositoryImpl: ObjectRepositoryImpl
    ): ObjectRepository

    // Futura implementação para produção
    // @Binds
    // @ProductionRepository
    // @Singleton
    // abstract fun bindProductionRepository(
    //     productionRepositoryImpl: ProductionObjectRepositoryImpl
    // ): ObjectRepository
}

// ========== QUALIFIERS ==========

/**
 * Qualifier para implementação de desenvolvimento
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DevelopmentRepository

/**
 * Qualifier para implementação de produção
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ProductionRepository

/**
 * Qualifier para cache local
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LocalCache

/**
 * Qualifier para cache remoto
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RemoteCache

// ========== DATA CLASSES DE CONFIGURAÇÃO ==========

/**
 * Configuração do sistema de cache
 */
data class CacheConfig(
    val maxSize: Int,
    val enableDebugLogs: Boolean,
    val autoCleanupEnabled: Boolean,
    val confidenceThreshold: Float,
    val maxObjectAge: Long
)

/**
 * Configuração do ARCore
 */
data class ARConfig(
    val enableCloudAnchors: Boolean,
    val enableLightEstimation: Boolean,
    val preferredFPS: Int,
    val autoFocus: Boolean,
    val imageStabilization: Boolean
)

/**
 * Configuração de detecção
 */
data class DetectionConfig(
    val minConfidenceThreshold: Float,
    val maxObjectsPerFrame: Int,
    val enableObjectTracking: Boolean,
    val trackingTimeout: Long,
    val enableMockData: Boolean
)

/**
 * Configuração de medições
 */
data class MeasurementConfig(
    val defaultLengthUnit: MeasurementUnit,
    val defaultWeightUnit: MeasurementUnit,
    val defaultVolumeUnit: MeasurementUnit,
    val precision: Int,
    val enableAutoUnitConversion: Boolean
)

// ========== MÓDULOS FUTUROS (PREPARAÇÃO) ==========

/**
 * Módulo para integrações AR (DIA 5)
 */
// @Module
// @InstallIn(SingletonComponent::class)
// abstract class ARModule {
//
//     @Binds
//     @Singleton
//     abstract fun bindARSession(
//         arSessionImpl: ARSessionImpl
//     ): ARSession
//
//     @Binds
//     @Singleton
//     abstract fun bindARRenderer(
//         arRendererImpl: ARRendererImpl
//     ): ARRenderer
// }

/**
 * Módulo para ML Kit (DIA 6)
 */
// @Module
// @InstallIn(SingletonComponent::class)
// abstract class MLModule {
//
//     @Binds
//     @Singleton
//     abstract fun bindObjectDetector(
//         objectDetectorImpl: MLKitObjectDetectorImpl
//     ): ObjectDetector
//
//     @Binds
//     @Singleton
//     abstract fun bindImageClassifier(
//         imageClassifierImpl: MLKitImageClassifierImpl
//     ): ImageClassifier
// }

/**
 * Módulo para persistência (DIA 7)
 */
// @Module
// @InstallIn(SingletonComponent::class)
// abstract class StorageModule {
//
//     @Binds
//     @Singleton
//     abstract fun bindLocalStorage(
//         localStorageImpl: RoomLocalStorageImpl
//     ): LocalStorage
//
//     @Binds
//     @Singleton
//     abstract fun bindPreferences(
//         preferencesImpl: DataStorePreferencesImpl
//     ): UserPreferences
// }

/**
 * Módulo para rede e sincronização (DIA 8)
 */
// @Module
// @InstallIn(SingletonComponent::class)
// abstract class NetworkModule {
//
//     @Provides
//     @Singleton
//     fun provideOkHttpClient(): OkHttpClient {
//         return OkHttpClient.Builder()
//             .addInterceptor(HttpLoggingInterceptor().apply {
//                 level = if (BuildConfig.DEBUG)
//                     HttpLoggingInterceptor.Level.BODY
//                 else
//                     HttpLoggingInterceptor.Level.NONE
//             })
//             .build()
//     }
//
//     @Provides
//     @Singleton
//     fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
//         return Retrofit.Builder()
//             .baseUrl("https://api.objectmeasure.com/")
//             .client(okHttpClient)
//             .addConverterFactory(GsonConverterFactory.create())
//             .build()
//     }
// }

/**
 * Módulo para testes
 */
// @Module
// @TestInstallIn(
//     components = [SingletonComponent::class],
//     replaces = [DataModule::class]
// )
// abstract class TestDataModule {
//
//     @Binds
//     @Singleton
//     abstract fun bindTestRepository(
//         testRepositoryImpl: TestObjectRepositoryImpl
//     ): ObjectRepository
//
//     @Provides
//     @Singleton
//     fun provideTestCacheConfig(): CacheConfig {
//         return CacheConfig(
//             maxSize = 10,
//             enableDebugLogs = true,
//             autoCleanupEnabled = false,
//             confidenceThreshold = 0.5f,
//             maxObjectAge = Long.MAX_VALUE
//         )
//     }
// }