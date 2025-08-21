package com.objectmeasure.ar.domain.model

import com.objectmeasure.ar.core.util.*
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/**
 * Estatísticas para um tipo específico de objeto dentro do cache - VERSÃO APRIMORADA
 *
 * Melhorias implementadas:
 * - Validações robustas de dados
 * - Funcionalidades de formatação e análise
 * - Comparações e ordenação
 * - Integração com sistema de qualidade
 * - Métodos utilitários para UI
 * - Serialização para persistência
 * - Cálculos derivados inteligentes
 *
 * @version 2.2
 */
@Serializable
data class TypeStatistics(
    val objectType: ObjectType,
    val count: Int,
    val averageConfidence: Float,
    val latestDetection: Long,
    val earliestDetection: Long = latestDetection,
    val maxConfidence: Float = averageConfidence,
    val minConfidence: Float = averageConfidence,
    val reliableCount: Int = 0,
    val sessionCount: Int = count,
    val timestamp: Long = getCurrentTimeMillis()
) {
    init {
        require(count >= 0) { "Count cannot be negative: $count" }
        require(averageConfidence in 0f..1f) { "Average confidence must be between 0.0 and 1.0: $averageConfidence" }
        require(maxConfidence in 0f..1f) { "Max confidence must be between 0.0 and 1.0: $maxConfidence" }
        require(minConfidence in 0f..1f) { "Min confidence must be between 0.0 and 1.0: $minConfidence" }
        require(reliableCount <= count) { "Reliable count ($reliableCount) cannot exceed total count ($count)" }
        require(sessionCount <= count) { "Session count ($sessionCount) cannot exceed total count ($count)" }
        require(earliestDetection <= latestDetection) { "Earliest detection cannot be after latest detection" }
        require(minConfidence <= averageConfidence) { "Min confidence cannot exceed average confidence" }
        require(averageConfidence <= maxConfidence) { "Average confidence cannot exceed max confidence" }
    }

    // ========== COMPUTED PROPERTIES ==========

    /**
     * Taxa de confiabilidade (objetos confiáveis / total)
     */
    val reliabilityRate: Float
        get() = if (count > 0) reliableCount.toFloat() / count else 0f

    /**
     * Duração do período de detecções
     */
    val detectionSpan: Long
        get() = if (latestDetection > earliestDetection) latestDetection - earliestDetection else 0L

    /**
     * Idade da última detecção
     */
    val lastDetectionAge: Long
        get() = getCurrentTimeMillis() - latestDetection

    /**
     * Taxa de detecções por hora (estimativa)
     */
    val detectionsPerHour: Float
        get() {
            val spanHours = detectionSpan / (1000f * 60f * 60f)
            return if (spanHours > 0) count / spanHours else 0f
        }

    /**
     * Score de qualidade geral das estatísticas
     */
    val qualityScore: Float
        get() {
            var score = 0f
            var maxScore = 0f

            // Confidence quality (peso 0.4)
            maxScore += 0.4f
            score += averageConfidence * 0.4f

            // Reliability rate (peso 0.3)
            maxScore += 0.3f
            score += reliabilityRate * 0.3f

            // Sample size quality (peso 0.2)
            maxScore += 0.2f
            val sampleSizeScore = when {
                count >= 50 -> 1.0f
                count >= 20 -> 0.8f
                count >= 10 -> 0.6f
                count >= 5 -> 0.4f
                count >= 2 -> 0.2f
                else -> 0.1f
            }
            score += sampleSizeScore * 0.2f

            // Recency (peso 0.1)
            maxScore += 0.1f
            val recencyScore = when {
                lastDetectionAge < 60_000 -> 1.0f      // < 1 min
                lastDetectionAge < 300_000 -> 0.8f     // < 5 min
                lastDetectionAge < 1800_000 -> 0.6f    // < 30 min
                lastDetectionAge < 3600_000 -> 0.4f    // < 1 hour
                else -> 0.2f
            }
            score += recencyScore * 0.1f

            return if (maxScore > 0) score / maxScore else 0f
        }

    /**
     * Nível de qualidade categorizado
     */
    val qualityLevel: StatisticsQuality
        get() = when {
            qualityScore >= 0.9f -> StatisticsQuality.EXCELLENT
            qualityScore >= 0.75f -> StatisticsQuality.GOOD
            qualityScore >= 0.6f -> StatisticsQuality.FAIR
            qualityScore >= 0.4f -> StatisticsQuality.POOR
            else -> StatisticsQuality.VERY_POOR
        }

    /**
     * Variabilidade da confidence (diferença entre max e min)
     */
    val confidenceVariability: Float
        get() = maxConfidence - minConfidence

    /**
     * Indica se as estatísticas são estatisticamente significativas
     */
    val isStatisticallySignificant: Boolean
        get() = count >= 10 && confidenceVariability < 0.4f && averageConfidence >= 0.5f

    // ========== VALIDATION METHODS ==========

    /**
     * Verifica se as estatísticas são confiáveis
     */
    fun isReliable(minCount: Int = 5, minConfidence: Float = 0.7f): Boolean {
        return count >= minCount && averageConfidence >= minConfidence && reliabilityRate >= 0.6f
    }

    /**
     * Verifica se as detecções são recentes
     */
    fun isRecent(maxAgeMs: Long = 300_000): Boolean {
        return lastDetectionAge <= maxAgeMs
    }

    /**
     * Verifica se há atividade suficiente
     */
    fun hasSufficientActivity(): Boolean {
        return count >= 3 && (detectionsPerHour > 0.1f || detectionSpan < 3600_000) // 1 hour
    }

    /**
     * Verifica se o tipo é popular (muitas detecções)
     */
    fun isPopularType(): Boolean {
        return count >= 20 || detectionsPerHour >= 1.0f
    }

    // ========== COMPARISON METHODS ==========

    /**
     * Compara com outras estatísticas
     */
    fun compareTo(other: TypeStatistics): Int {
        // Priorizar por score de qualidade, depois por count
        val qualityComparison = qualityScore.compareTo(other.qualityScore)
        return if (qualityComparison != 0) {
            -qualityComparison // Maior qualidade primeiro
        } else {
            -count.compareTo(other.count) // Maior count primeiro
        }
    }

    /**
     * Calcula similaridade com outras estatísticas
     */
    fun similarityScore(other: TypeStatistics): Float {
        if (objectType != other.objectType) return 0f

        var score = 0f
        var factors = 0

        // Similaridade de confidence
        val confidenceDiff = kotlin.math.abs(averageConfidence - other.averageConfidence)
        score += (1f - confidenceDiff)
        factors++

        // Similaridade de reliability rate
        val reliabilityDiff = kotlin.math.abs(reliabilityRate - other.reliabilityRate)
        score += (1f - reliabilityDiff)
        factors++

        // Similaridade de count (normalizada)
        val maxCount = kotlin.math.max(count, other.count).toFloat()
        val minCount = kotlin.math.min(count, other.count).toFloat()
        val countSimilarity = if (maxCount > 0) minCount / maxCount else 1f
        score += countSimilarity
        factors++

        return if (factors > 0) score / factors else 0f
    }

    // ========== FORMATTING METHODS ==========

    /**
     * Formata confidence como porcentagem
     */
    fun getFormattedConfidence(): String {
        return "${(averageConfidence * 100).roundToInt()}%"
    }

    /**
     * Formata reliability rate como porcentagem
     */
    fun getFormattedReliabilityRate(): String {
        return "${(reliabilityRate * 100).roundToInt()}%"
    }

    /**
     * Formata idade da última detecção
     */
    fun getFormattedLastDetectionAge(): String {
        val ageSeconds = lastDetectionAge / 1000
        return when {
            ageSeconds < 60 -> "${ageSeconds}s atrás"
            ageSeconds < 3600 -> "${ageSeconds / 60}m atrás"
            ageSeconds < 86400 -> "${ageSeconds / 3600}h atrás"
            else -> "${ageSeconds / 86400}d atrás"
        }
    }

    /**
     * Formata duração do período de detecções
     */
    fun getFormattedDetectionSpan(): String {
        val spanSeconds = detectionSpan / 1000
        return when {
            spanSeconds < 60 -> "${spanSeconds}s"
            spanSeconds < 3600 -> "${spanSeconds / 60}m"
            spanSeconds < 86400 -> "${spanSeconds / 3600}h"
            else -> "${spanSeconds / 86400}d"
        }
    }

    /**
     * Formata taxa de detecções
     */
    fun getFormattedDetectionRate(): String {
        return when {
            detectionsPerHour >= 1.0f -> "${detectionsPerHour.roundToInt()}/h"
            detectionsPerHour >= 0.1f -> "${(detectionsPerHour * 60).roundToInt()}/h"
            else -> "${(detectionsPerHour * 1440).roundToInt()}/dia"
        }
    }

    /**
     * Resumo formatado das estatísticas
     */
    fun getSummary(): String {
        return "${objectType.displayName}: $count detecções (${getFormattedConfidence()} confiança)"
    }

    /**
     * Descrição detalhada
     */
    fun getDetailedDescription(): String {
        return buildString {
            append("${objectType.displayName}:")
            append(" $count detecções")
            append(" (${getFormattedConfidence()} conf.")
            append(", ${getFormattedReliabilityRate()} confiáveis")
            append(", última ${getFormattedLastDetectionAge()})")
            if (qualityLevel != StatisticsQuality.GOOD) {
                append(" [${qualityLevel.displayName}]")
            }
        }
    }

    // ========== ANALYSIS METHODS ==========

    /**
     * Detecta trends ou padrões
     */
    fun getTrend(): StatisticsTrend {
        return when {
            lastDetectionAge > 3600_000 -> StatisticsTrend.DECLINING // > 1 hour
            detectionsPerHour >= 2.0f -> StatisticsTrend.RISING
            detectionsPerHour >= 0.5f -> StatisticsTrend.STABLE
            count < 3 -> StatisticsTrend.INSUFFICIENT_DATA
            else -> StatisticsTrend.DECLINING
        }
    }

    /**
     * Recomendações baseadas nas estatísticas
     */
    fun getRecommendations(): List<String> {
        val recommendations = mutableListOf<String>()

        if (averageConfidence < 0.6f) {
            recommendations.add("Melhorar qualidade de detecção para ${objectType.displayName}")
        }

        if (reliabilityRate < 0.5f) {
            recommendations.add("Ajustar parâmetros de confidence para ${objectType.displayName}")
        }

        if (count < 5) {
            recommendations.add("Coletar mais amostras de ${objectType.displayName}")
        }

        if (lastDetectionAge > 1800_000) { // 30 min
            recommendations.add("${objectType.displayName} não detectado recentemente")
        }

        if (confidenceVariability > 0.5f) {
            recommendations.add("Inconsistência na detecção de ${objectType.displayName}")
        }

        return recommendations
    }

    /**
     * Score de importância relativa
     */
    fun getImportanceScore(): Float {
        var score = 0f

        // Frequência de uso
        score += kotlin.math.min(count / 50f, 1f) * 0.4f

        // Qualidade das detecções
        score += averageConfidence * 0.3f

        // Recência
        val recencyScore = when {
            lastDetectionAge < 300_000 -> 1.0f     // 5 min
            lastDetectionAge < 1800_000 -> 0.7f    // 30 min
            lastDetectionAge < 3600_000 -> 0.4f    // 1 hour
            else -> 0.1f
        }
        score += recencyScore * 0.2f

        // Confiabilidade
        score += reliabilityRate * 0.1f

        return score.coerceIn(0f, 1f)
    }

    // ========== EXPORT/IMPORT ==========

    /**
     * Exporta para formato serializado
     */
    fun toExportData(): Map<String, Any> {
        return mapOf(
            "objectType" to objectType.name,
            "count" to count,
            "averageConfidence" to averageConfidence,
            "latestDetection" to latestDetection,
            "earliestDetection" to earliestDetection,
            "maxConfidence" to maxConfidence,
            "minConfidence" to minConfidence,
            "reliableCount" to reliableCount,
            "sessionCount" to sessionCount,
            "qualityScore" to qualityScore,
            "summary" to getSummary()
        )
    }

    companion object {
        private fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

        /**
         * Cria estatísticas vazias para um tipo
         */
        fun empty(objectType: ObjectType): TypeStatistics {
            return TypeStatistics(
                objectType = objectType,
                count = 0,
                averageConfidence = 0f,
                latestDetection = 0L,
                earliestDetection = 0L,
                maxConfidence = 0f,
                minConfidence = 0f,
                reliableCount = 0,
                sessionCount = 0
            )
        }

        /**
         * Cria estatísticas de uma única detecção
         */
        fun fromSingleDetection(
            objectType: ObjectType,
            confidence: Float,
            timestamp: Long = getCurrentTimeMillis(),
            isReliable: Boolean = confidence >= 0.7f
        ): TypeStatistics {
            require(confidence in 0f..1f) { "Confidence must be between 0.0 and 1.0" }

            return TypeStatistics(
                objectType = objectType,
                count = 1,
                averageConfidence = confidence,
                latestDetection = timestamp,
                earliestDetection = timestamp,
                maxConfidence = confidence,
                minConfidence = confidence,
                reliableCount = if (isReliable) 1 else 0,
                sessionCount = 1
            )
        }

        /**
         * Combina múltiplas estatísticas do mesmo tipo
         */
        fun combine(statistics: List<TypeStatistics>): TypeStatistics? {
            if (statistics.isEmpty()) return null

            val firstType = statistics.first().objectType
            require(statistics.all { it.objectType == firstType }) {
                "All statistics must be for the same object type"
            }

            val totalCount = statistics.sumOf { it.count }
            if (totalCount == 0) return empty(firstType)

            val weightedConfidenceSum = statistics.sumOf { it.averageConfidence * it.count }
            val avgConfidence = weightedConfidenceSum / totalCount

            return TypeStatistics(
                objectType = firstType,
                count = totalCount,
                averageConfidence = avgConfidence.toFloat(),
                latestDetection = statistics.maxOf { it.latestDetection },
                earliestDetection = statistics.minOf { it.earliestDetection },
                maxConfidence = statistics.maxOf { it.maxConfidence },
                minConfidence = statistics.minOf { it.minConfidence },
                reliableCount = statistics.sumOf { it.reliableCount },
                sessionCount = statistics.sumOf { it.sessionCount }
            )
        }

        /**
         * Cria estatísticas mock para testes
         */
        fun createMock(
            objectType: ObjectType = ObjectType.PHONE,
            count: Int = 10,
            averageConfidence: Float = 0.8f
        ): TypeStatistics {
            val currentTime = getCurrentTimeMillis()
            return TypeStatistics(
                objectType = objectType,
                count = count,
                averageConfidence = averageConfidence,
                latestDetection = currentTime,
                earliestDetection = currentTime - (count * 60_000L), // 1 min per detection
                maxConfidence = (averageConfidence + 0.1f).coerceAtMost(1f),
                minConfidence = (averageConfidence - 0.1f).coerceAtLeast(0f),
                reliableCount = (count * 0.8f).roundToInt(),
                sessionCount = count
            )
        }

        /**
         * Importa de dados serializados
         */
        fun fromExportData(data: Map<String, Any>): TypeStatistics? {
            return try {
                TypeStatistics(
                    objectType = ObjectType.valueOf(data["objectType"] as String),
                    count = (data["count"] as Number).toInt(),
                    averageConfidence = (data["averageConfidence"] as Number).toFloat(),
                    latestDetection = (data["latestDetection"] as Number).toLong(),
                    earliestDetection = (data["earliestDetection"] as Number).toLong(),
                    maxConfidence = (data["maxConfidence"] as Number).toFloat(),
                    minConfidence = (data["minConfidence"] as Number).toFloat(),
                    reliableCount = (data["reliableCount"] as Number).toInt(),
                    sessionCount = (data["sessionCount"] as Number).toInt()
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

// ========== ENUMS ==========

@Serializable
enum class StatisticsQuality(val displayName: String, val color: String) {
    EXCELLENT("Excelente", "#4CAF50"),
    GOOD("Boa", "#8BC34A"),
    FAIR("Razoável", "#FF9800"),
    POOR("Ruim", "#FF5722"),
    VERY_POOR("Muito Ruim", "#F44336")
}

@Serializable
enum class StatisticsTrend(val displayName: String, val icon: String) {
    RISING("Crescendo", "↗️"),
    STABLE("Estável", "→"),
    DECLINING("Declinando", "↘️"),
    INSUFFICIENT_DATA("Dados Insuficientes", "❓")
}

// ========== EXTENSION FUNCTIONS ==========

/**
 * Extensões para listas de TypeStatistics
 */
fun List<TypeStatistics>.getMostPopular(): TypeStatistics? {
    return maxByOrNull { it.count }
}

fun List<TypeStatistics>.getMostReliable(): TypeStatistics? {
    return maxByOrNull { it.qualityScore }
}

fun List<TypeStatistics>.getMostRecent(): TypeStatistics? {
    return maxByOrNull { it.latestDetection }
}

fun List<TypeStatistics>.filterReliable(minQuality: StatisticsQuality = StatisticsQuality.FAIR): List<TypeStatistics> {
    return filter { it.qualityLevel >= minQuality }
}

fun List<TypeStatistics>.filterRecent(maxAgeMs: Long = 1800_000): List<TypeStatistics> {
    return filter { it.isRecent(maxAgeMs) }
}

fun List<TypeStatistics>.sortedByImportance(): List<TypeStatistics> {
    return sortedByDescending { it.getImportanceScore() }
}

fun List<TypeStatistics>.getTotalDetections(): Int {
    return sumOf { it.count }
}

fun List<TypeStatistics>.getAverageQuality(): Float {
    return if (isNotEmpty()) map { it.qualityScore }.average().toFloat() else 0f
}

/**
 * Agrupa estatísticas por categoria de objeto
 */
fun List<TypeStatistics>.groupByCategory(): Map<ObjectCategory, List<TypeStatistics>> {
    return groupBy { it.objectType.category }
}

/**
 * Encontra estatísticas para um tipo específico
 */
fun List<TypeStatistics>.findByType(objectType: ObjectType): TypeStatistics? {
    return find { it.objectType == objectType }
}

/**
 * Calcula distribuição percentual
 */
fun List<TypeStatistics>.getPercentageDistribution(): Map<ObjectType, Float> {
    val total = getTotalDetections()
    return if (total > 0) {
        associate { it.objectType to (it.count.toFloat() / total * 100f) }
    } else {
        emptyMap()
    }
}