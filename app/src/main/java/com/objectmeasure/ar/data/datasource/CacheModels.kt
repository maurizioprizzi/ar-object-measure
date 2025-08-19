package com.objectmeasure.ar.data.datasource

import com.objectmeasure.ar.domain.model.ObjectType

/**
 * Modelos de dados relacionados ao CacheDataSource.
 * Separa as estruturas de dados da lógica de implementação do cache.
 */

/**
 * Contém as estatísticas agregadas do estado atual do cache.
 */
data class CacheStatistics(
    val totalObjects: Int,
    val reliableObjects: Int,
    val averageConfidence: Float,
    val maxConfidence: Float,
    val minConfidence: Float,
    val typeDistribution: Map<ObjectType, Int>,
    val oldestTimestamp: Long,
    val newestTimestamp: Long,
    val cacheUtilization: Float // Porcentagem de uso do cache (0.0 a 1.0)
) {
    val reliabilityRate: Float
        get() = if (totalObjects > 0) reliableObjects.toFloat() / totalObjects else 0f

    val sessionDuration: Long
        get() = if (newestTimestamp > oldestTimestamp) newestTimestamp - oldestTimestamp else 0L

    val utilizationPercentage: Float
        get() = (cacheUtilization * 100).coerceAtMost(100f)

    val mostCommonType: ObjectType?
        get() = typeDistribution.maxByOrNull { it.value }?.key
}

/**
 * Representa a configuração atual do cache.
 */
data class CacheConfiguration(
    val maxSize: Int,
    val autoCleanupEnabled: Boolean,
    val confidenceThreshold: Float,
    val currentSize: Int
) {
    val isFull: Boolean
        get() = currentSize >= maxSize

    val fillPercentage: Float
        get() = if (maxSize > 0) {
            (currentSize.toFloat() / maxSize * 100).coerceAtMost(100f)
        } else 0f
}