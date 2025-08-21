package com.objectmeasure.ar.data.datasource

import com.objectmeasure.ar.domain.model.ObjectType
import kotlin.math.roundToInt

/**
 * Modelos de dados relacionados ao CacheDataSource - VERSÃO CONSOLIDADA E OTIMIZADA
 *
 * Melhorias implementadas:
 * - Propriedades computadas otimizadas e thread-safe
 * - Validações robustas para evitar divisões por zero
 * - Factory methods para construção segura
 * - Formatação para UI integrada
 * - Performance otimizada para operações frequentes
 * - Funcionalidades estendidas
 *
 * @version 2.2
 */

/**
 * Contém as estatísticas agregadas do estado atual do cache.
 * Versão consolidada com todas as funcionalidades e otimizações.
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
    val cacheUtilization: Float, // 0.0 a 1.0
    val memoryUsageEstimate: Long = 0L // em bytes
) {
    init {
        require(totalObjects >= 0) { "Total objects cannot be negative: $totalObjects" }
        require(reliableObjects >= 0) { "Reliable objects cannot be negative: $reliableObjects" }
        require(reliableObjects <= totalObjects) { "Reliable objects ($reliableObjects) cannot exceed total ($totalObjects)" }
        require(cacheUtilization in 0.0f..1.0f) { "Cache utilization must be between 0.0 and 1.0: $cacheUtilization" }
        require(averageConfidence in 0.0f..1.0f || (totalObjects == 0 && averageConfidence == 0.0f)) {
            "Average confidence must be between 0.0 and 1.0: $averageConfidence"
        }
    }

    /**
     * Taxa de confiabilidade (objetos confiáveis / total).
     * Thread-safe e protegido contra divisão por zero.
     */
    val reliabilityRate: Float
        get() = if (totalObjects > 0) {
            (reliableObjects.toFloat() / totalObjects).coerceIn(0f, 1f)
        } else 0f

    /**
     * Duração da sessão baseada nos timestamps mais antigo e recente.
     */
    val sessionDuration: Long
        get() = if (newestTimestamp > oldestTimestamp && oldestTimestamp > 0) {
            newestTimestamp - oldestTimestamp
        } else 0L

    /**
     * Porcentagem de utilização do cache (0-100%).
     */
    val utilizationPercentage: Float
        get() = (cacheUtilization * 100f).coerceIn(0f, 100f)

    /**
     * Tipo de objeto mais comum no cache.
     * Otimizada para evitar múltiplas passagens.
     */
    val mostCommonType: ObjectType?
        get() = if (typeDistribution.isEmpty()) {
            null
        } else {
            var maxType: ObjectType? = null
            var maxCount = 0
            typeDistribution.forEach { (type, count) ->
                if (count > maxCount) {
                    maxCount = count
                    maxType = type
                }
            }
            maxType
        }

    /**
     * Contagem do tipo mais comum.
     */
    val mostCommonTypeCount: Int
        get() = mostCommonType?.let { typeDistribution[it] } ?: 0

    /**
     * Lista dos tipos ordenados por frequência (descendente).
     */
    val typesByFrequency: List<Pair<ObjectType, Int>>
        get() = typeDistribution.toList().sortedByDescending { it.second }

    /**
     * Diversidade de tipos (quantos tipos diferentes existem).
     */
    val typesDiversity: Int
        get() = typeDistribution.keys.size

    /**
     * Taxa de confiança média formatada para UI.
     */
    val formattedAverageConfidence: String
        get() = "${(averageConfidence * 100).roundToInt()}%"

    /**
     * Utilização formatada para UI.
     */
    val formattedUtilization: String
        get() = "${utilizationPercentage.roundToInt()}%"

    /**
     * Duração da sessão formatada.
     */
    val formattedSessionDuration: String
        get() {
            val duration = sessionDuration
            return when {
                duration < 1000 -> "${duration}ms"
                duration < 60_000 -> "${(duration / 1000f).roundToInt()}s"
                duration < 3600_000 -> "${(duration / 60_000f).roundToInt()}m"
                else -> "${(duration / 3600_000f).roundToInt()}h"
            }
        }

    /**
     * Uso de memória formatado.
     */
    val formattedMemoryUsage: String
        get() {
            return when {
                memoryUsageEstimate < 1024 -> "${memoryUsageEstimate}B"
                memoryUsageEstimate < 1024 * 1024 -> "${(memoryUsageEstimate / 1024f).roundToInt()}KB"
                memoryUsageEstimate < 1024 * 1024 * 1024 -> "${(memoryUsageEstimate / (1024f * 1024f)).roundToInt()}MB"
                else -> "${(memoryUsageEstimate / (1024f * 1024f * 1024f)).roundToInt()}GB"
            }
        }

    /**
     * Verifica se o cache tem boa performance.
     */
    val hasGoodPerformance: Boolean
        get() = reliabilityRate >= 0.8f && averageConfidence >= 0.7f

    /**
     * Verifica se está na hora de fazer limpeza.
     */
    val needsCleanup: Boolean
        get() = cacheUtilization > 0.8f || reliabilityRate < 0.6f

    /**
     * Score geral de qualidade do cache (0-100).
     */
    val qualityScore: Int
        get() {
            val reliabilityWeight = 0.4f
            val confidenceWeight = 0.3f
            val utilizationWeight = 0.2f
            val diversityWeight = 0.1f

            val normalizedDiversity = if (typesDiversity > 0) {
                (typesDiversity.toFloat() / ObjectType.values().size).coerceAtMost(1f)
            } else 0f

            val score = (reliabilityRate * reliabilityWeight +
                    averageConfidence * confidenceWeight +
                    (1f - cacheUtilization) * utilizationWeight + // Menos utilização = melhor
                    normalizedDiversity * diversityWeight) * 100f

            return score.roundToInt().coerceIn(0, 100)
        }

    companion object {
        /**
         * Cria estatísticas vazias para cache vazio.
         */
        fun empty(maxCacheSize: Int) = CacheStatistics(
            totalObjects = 0,
            reliableObjects = 0,
            averageConfidence = 0f,
            maxConfidence = 0f,
            minConfidence = 0f,
            typeDistribution = emptyMap(),
            oldestTimestamp = 0L,
            newestTimestamp = 0L,
            cacheUtilization = 0f,
            memoryUsageEstimate = 0L
        )

        /**
         * Cria estatísticas de exemplo para testes.
         */
        fun sample() = CacheStatistics(
            totalObjects = 25,
            reliableObjects = 20,
            averageConfidence = 0.85f,
            maxConfidence = 0.95f,
            minConfidence = 0.65f,
            typeDistribution = mapOf(
                ObjectType.PERSON to 10,
                ObjectType.VEHICLE to 8,
                ObjectType.OBJECT to 7
            ),
            oldestTimestamp = System.currentTimeMillis() - 300_000, // 5 min atrás
            newestTimestamp = System.currentTimeMillis(),
            cacheUtilization = 0.5f,
            memoryUsageEstimate = 5120L // 5KB
        )
    }
}

/**
 * Representa a configuração atual do cache.
 * Versão estendida com todas as configurações e validações.
 */
data class CacheConfiguration(
    val maxSize: Int,
    val autoCleanupEnabled: Boolean,
    val confidenceThreshold: Float,
    val currentSize: Int,
    val maxObjectAgeMs: Long = 5 * 60 * 1000L, // 5 minutos default
    val duplicateDetectionEnabled: Boolean = true,
    val cleanupIntervalMs: Long = 30_000L // 30 segundos default
) {
    init {
        require(maxSize > 0) { "Max size must be positive: $maxSize" }
        require(currentSize >= 0) { "Current size cannot be negative: $currentSize" }
        require(confidenceThreshold in 0.0f..1.0f) { "Confidence threshold must be between 0.0 and 1.0: $confidenceThreshold" }
        require(maxObjectAgeMs > 0) { "Max object age must be positive: $maxObjectAgeMs" }
        require(cleanupIntervalMs > 0) { "Cleanup interval must be positive: $cleanupIntervalMs" }
    }

    /**
     * Verifica se o cache está cheio.
     */
    val isFull: Boolean
        get() = currentSize >= maxSize

    /**
     * Verifica se está quase cheio (>80%).
     */
    val isNearlyFull: Boolean
        get() = currentSize >= (maxSize * 0.8f).toInt()

    /**
     * Porcentagem de preenchimento (0-100%).
     */
    val fillPercentage: Float
        get() = if (maxSize > 0) {
            (currentSize.toFloat() / maxSize * 100f).coerceIn(0f, 100f)
        } else 0f

    /**
     * Espaço livre disponível.
     */
    val freeSpace: Int
        get() = (maxSize - currentSize).coerceAtLeast(0)

    /**
     * Porcentagem de espaço livre.
     */
    val freeSpacePercentage: Float
        get() = if (maxSize > 0) {
            (freeSpace.toFloat() / maxSize * 100f).coerceIn(0f, 100f)
        } else 0f

    /**
     * Nível de preenchimento categorizado.
     */
    val fillLevel: FillLevel
        get() = when {
            fillPercentage < 25f -> FillLevel.LOW
            fillPercentage < 50f -> FillLevel.MODERATE
            fillPercentage < 75f -> FillLevel.HIGH
            fillPercentage < 90f -> FillLevel.VERY_HIGH
            else -> FillLevel.CRITICAL
        }

    /**
     * Idade máxima formatada para UI.
     */
    val formattedMaxAge: String
        get() {
            return when {
                maxObjectAgeMs < 60_000 -> "${maxObjectAgeMs / 1000}s"
                maxObjectAgeMs < 3600_000 -> "${maxObjectAgeMs / 60_000}m"
                else -> "${maxObjectAgeMs / 3600_000}h"
            }
        }

    /**
     * Preenchimento formatado para UI.
     */
    val formattedFillPercentage: String
        get() = "${fillPercentage.roundToInt()}%"

    /**
     * Status geral do cache.
     */
    val status: CacheStatus
        get() = when {
            !autoCleanupEnabled && isFull -> CacheStatus.ERROR
            isNearlyFull && !autoCleanupEnabled -> CacheStatus.WARNING
            fillLevel == FillLevel.CRITICAL -> CacheStatus.WARNING
            hasOptimalSettings -> CacheStatus.OPTIMAL
            else -> CacheStatus.NORMAL
        }

    /**
     * Verifica se as configurações estão otimizadas.
     */
    val hasOptimalSettings: Boolean
        get() = autoCleanupEnabled &&
                duplicateDetectionEnabled &&
                confidenceThreshold in 0.7f..0.9f &&
                maxObjectAgeMs in 60_000L..600_000L && // 1-10 minutos
                fillPercentage < 80f

    /**
     * Recomendações de configuração.
     */
    val recommendations: List<String>
        get() = buildList {
            if (!autoCleanupEnabled && fillPercentage > 70f) {
                add("Habilitar limpeza automática para melhor performance")
            }
            if (!duplicateDetectionEnabled) {
                add("Habilitar detecção de duplicatas para economizar memória")
            }
            if (confidenceThreshold < 0.5f) {
                add("Aumentar threshold de confidence para melhor qualidade")
            }
            if (confidenceThreshold > 0.9f) {
                add("Diminuir threshold de confidence para capturar mais objetos")
            }
            if (maxObjectAgeMs < 30_000L) {
                add("Aumentar idade máxima dos objetos para melhor histórico")
            }
            if (maxObjectAgeMs > 600_000L) {
                add("Diminuir idade máxima dos objetos para melhor performance")
            }
            if (maxSize < 20) {
                add("Aumentar tamanho máximo do cache para melhor tracking")
            }
            if (maxSize > 200) {
                add("Diminuir tamanho máximo do cache para melhor performance")
            }
        }

    companion object {
        /**
         * Configuração padrão otimizada.
         */
        fun default() = CacheConfiguration(
            maxSize = 50,
            autoCleanupEnabled = true,
            confidenceThreshold = 0.7f,
            currentSize = 0,
            maxObjectAgeMs = 5 * 60 * 1000L,
            duplicateDetectionEnabled = true,
            cleanupIntervalMs = 30_000L
        )

        /**
         * Configuração para alta performance (menos objetos, mais seletivo).
         */
        fun highPerformance() = CacheConfiguration(
            maxSize = 30,
            autoCleanupEnabled = true,
            confidenceThreshold = 0.8f,
            currentSize = 0,
            maxObjectAgeMs = 3 * 60 * 1000L,
            duplicateDetectionEnabled = true,
            cleanupIntervalMs = 20_000L
        )

        /**
         * Configuração para máxima cobertura (mais objetos, menos seletivo).
         */
        fun maxCoverage() = CacheConfiguration(
            maxSize = 100,
            autoCleanupEnabled = true,
            confidenceThreshold = 0.6f,
            currentSize = 0,
            maxObjectAgeMs = 10 * 60 * 1000L,
            duplicateDetectionEnabled = false,
            cleanupIntervalMs = 60_000L
        )
    }
}

/**
 * Nível de preenchimento do cache.
 */
enum class FillLevel(val description: String, val color: String) {
    LOW("Baixo", "#4CAF50"),           // Verde
    MODERATE("Moderado", "#8BC34A"),   // Verde claro
    HIGH("Alto", "#FF9800"),           // Laranja
    VERY_HIGH("Muito Alto", "#FF5722"), // Laranja escuro
    CRITICAL("Crítico", "#F44336")     // Vermelho
}

/**
 * Status geral do cache.
 */
enum class CacheStatus(val description: String, val color: String) {
    OPTIMAL("Ótimo", "#4CAF50"),       // Verde
    NORMAL("Normal", "#2196F3"),       // Azul
    WARNING("Atenção", "#FF9800"),     // Laranja
    ERROR("Erro", "#F44336")           // Vermelho
}

/**
 * Resumo executivo das estatísticas e configurações do cache.
 */
data class CacheHealthReport(
    val statistics: CacheStatistics,
    val configuration: CacheConfiguration,
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Saúde geral do cache (0-100).
     */
    val overallHealth: Int
        get() {
            val statsScore = statistics.qualityScore * 0.7f
            val configScore = if (configuration.hasOptimalSettings) 100f else 70f
            val configWeight = 0.3f

            return (statsScore + configScore * configWeight).roundToInt().coerceIn(0, 100)
        }

    /**
     * Status geral combinado.
     */
    val overallStatus: CacheStatus
        get() = when {
            overallHealth >= 90 -> CacheStatus.OPTIMAL
            overallHealth >= 70 -> CacheStatus.NORMAL
            overallHealth >= 50 -> CacheStatus.WARNING
            else -> CacheStatus.ERROR
        }

    /**
     * Principais alertas do sistema.
     */
    val alerts: List<String>
        get() = buildList {
            if (statistics.needsCleanup) {
                add("Cache precisa de limpeza")
            }
            if (configuration.isFull) {
                add("Cache está cheio")
            }
            if (!statistics.hasGoodPerformance) {
                add("Performance do cache abaixo do esperado")
            }
            if (configuration.status == CacheStatus.ERROR) {
                add("Configuração do cache tem problemas")
            }
            if (statistics.reliabilityRate < 0.5f) {
                add("Taxa de confiabilidade muito baixa")
            }
        }

    /**
     * Relatório formatado para UI.
     */
    val formattedReport: String
        get() = buildString {
            appendLine("=== RELATÓRIO DE SAÚDE DO CACHE ===")
            appendLine("Saúde Geral: $overallHealth% (${overallStatus.description})")
            appendLine()
            appendLine("ESTATÍSTICAS:")
            appendLine("• Objetos: ${statistics.totalObjects} (${statistics.formattedUtilization})")
            appendLine("• Confiabilidade: ${statistics.formattedAverageConfidence}")
            appendLine("• Memória: ${statistics.formattedMemoryUsage}")
            appendLine("• Duração: ${statistics.formattedSessionDuration}")
            appendLine()
            appendLine("CONFIGURAÇÃO:")
            appendLine("• Tamanho: ${configuration.currentSize}/${configuration.maxSize}")
            appendLine("• Threshold: ${(configuration.confidenceThreshold * 100).roundToInt()}%")
            appendLine("• Auto-limpeza: ${if (configuration.autoCleanupEnabled) "Ativa" else "Inativa"}")
            appendLine("• Idade máxima: ${configuration.formattedMaxAge}")

            if (alerts.isNotEmpty()) {
                appendLine()
                appendLine("ALERTAS:")
                alerts.forEach { alert ->
                    appendLine("⚠️ $alert")
                }
            }

            if (configuration.recommendations.isNotEmpty()) {
                appendLine()
                appendLine("RECOMENDAÇÕES:")
                configuration.recommendations.forEach { rec ->
                    appendLine("💡 $rec")
                }
            }
        }
}