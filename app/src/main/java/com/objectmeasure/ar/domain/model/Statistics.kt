package com.objectmeasure.ar.domain.model


/**
 * Contém estatísticas para um tipo de objeto específico dentro do cache.
 */
data class TypeStatistics(
    val count: Int,
    val averageConfidence: Float,
    val latestDetection: Long
)