package com.objectmeasure.ar.core.util

import com.objectmeasure.ar.BuildConfig
import com.objectmeasure.ar.domain.model.DetectedObject
import com.objectmeasure.ar.domain.model.ObjectType
import com.objectmeasure.ar.domain.model.TypeStatistics
import com.objectmeasure.ar.domain.repository.BoundingBox
import kotlin.math.*
import kotlin.random.Random
import kotlinx.coroutines.delay

/**
 * Extensões utilitárias para AR Object Measure - VERSÃO REVISADA E OTIMIZADA
 *
 * Principais melhorias:
 * - Otimizações de performance
 * - Validações mais robustas
 * - Melhor eficiência de memória
 * - Tratamento de erros aprimorado
 * - Funções adicionais úteis
 * - NULL SAFETY CORRIGIDO
 *
 * @version 2.1
 * @author AR Team
 */

// ========== STRING EXTENSIONS ==========

/**
 * Verifica se a string representa um número válido.
 * Agora suporta números negativos e notação científica.
 */
fun String?.isValidNumber(): Boolean {
    if (this.isNullOrBlank()) return false
    return try {
        this.toDouble()
        true
    } catch (e: NumberFormatException) {
        false
    }
}

/**
 * Conversão segura para Float com valor padrão.
 */
fun String?.toFloatSafe(default: Float = 0f): Float {
    return this?.toFloatOrNull() ?: default
}

/**
 * Conversão segura para Double com valor padrão.
 */
fun String?.toDoubleSafe(default: Double = 0.0): Double {
    return this?.toDoubleOrNull() ?: default
}

/**
 * Verifica se a string contém apenas dígitos.
 */
fun String?.isDigitsOnly(): Boolean {
    return !this.isNullOrEmpty() && this.all { it.isDigit() }
}

// ========== NUMERIC EXTENSIONS ==========

/**
 * Verifica se o valor está dentro de uma faixa com tolerância.
 */
fun Float.isInRangeWithTolerance(target: Float, tolerance: Float): Boolean {
    return this in (target - tolerance)..(target + tolerance)
}

/**
 * Arredonda para um número específico de casas decimais.
 * Versão otimizada usando multiplicação em vez de pow.
 */
fun Float.roundTo(decimals: Int): Float {
    if (decimals < 0) return this
    val multiplier = when (decimals) {
        0 -> 1f
        1 -> 10f
        2 -> 100f
        3 -> 1000f
        else -> 10f.pow(decimals)
    }
    return round(this * multiplier) / multiplier
}

/**
 * Versão otimizada do roundTo para Double.
 */
fun Double.roundTo(decimals: Int): Double {
    if (decimals < 0) return this
    val multiplier = when (decimals) {
        0 -> 1.0
        1 -> 10.0
        2 -> 100.0
        3 -> 1000.0
        else -> 10.0.pow(decimals)
    }
    return round(this * multiplier) / multiplier
}

/**
 * Clamp entre valores mínimo e máximo.
 */
fun Float.clamp(min: Float, max: Float): Float = coerceIn(min, max)
fun Double.clamp(min: Double, max: Double): Double = coerceIn(min, max)

/**
 * Verifica se o número é aproximadamente igual a outro.
 */
fun Float.isApproximatelyEqual(other: Float, epsilon: Float = 1e-6f): Boolean {
    return abs(this - other) < epsilon
}

// ========== CONFIDENCE EXTENSIONS ==========

/**
 * Verifica se o valor representa uma confiança válida (0.0 a 1.0).
 */
fun Float.isValidConfidence(): Boolean {
    return this in 0.0f..1.0f && !this.isNaN()
}

/**
 * Normaliza a confiança para o intervalo válido.
 */
fun Float.normalizeConfidence(): Float {
    return when {
        this.isNaN() -> 0.0f
        this < 0.0f -> 0.0f
        this > 1.0f -> 1.0f
        else -> this
    }
}

/**
 * Verifica se a confiança é considerada alta.
 */
fun Float.isHighConfidence(threshold: Float = 0.8f): Boolean {
    return isValidConfidence() && this >= threshold
}

/**
 * Categoriza o nível de confiança.
 */
enum class ConfidenceLevel { LOW, MEDIUM, HIGH, VERY_HIGH }

fun Float.getConfidenceLevel(): ConfidenceLevel {
    return when {
        this < 0.3f -> ConfidenceLevel.LOW
        this < 0.6f -> ConfidenceLevel.MEDIUM
        this < 0.9f -> ConfidenceLevel.HIGH
        else -> ConfidenceLevel.VERY_HIGH
    }
}

// ========== BOUNDING BOX EXTENSIONS - NULL SAFE ==========

val BoundingBox.centerX: Float
    get() = (left + right) / 2f

val BoundingBox.centerY: Float
    get() = (top + bottom) / 2f

val BoundingBox.area: Float
    get() = width() * height()

val BoundingBox.diagonal: Float
    get() = sqrt(width().pow(2) + height().pow(2))

val BoundingBox.aspectRatio: Float
    get() = if (height() > 0) width() / height() else 0f

/**
 * Versões NULL SAFE das propriedades do BoundingBox
 */
val BoundingBox?.safeCenterX: Float
    get() = this?.centerX ?: 0f

val BoundingBox?.safeCenterY: Float
    get() = this?.centerY ?: 0f

val BoundingBox?.safeArea: Float
    get() = this?.area ?: 0f

val BoundingBox?.safeDiagonal: Float
    get() = this?.diagonal ?: 0f

val BoundingBox?.safeAspectRatio: Float
    get() = this?.aspectRatio ?: 0f

/**
 * Verifica se o bounding box é aproximadamente quadrado.
 */
fun BoundingBox?.isSquare(tolerance: Float = 0.1f): Boolean {
    return this?.aspectRatio?.isInRangeWithTolerance(1.0f, tolerance) ?: false
}

/**
 * Verifica se o bounding box é válido (dimensões positivas).
 */
fun BoundingBox?.isValid(): Boolean {
    return this?.let { box ->
        box.width() > 0 && box.height() > 0 && box.left >= 0 && box.top >= 0
    } ?: false
}

/**
 * Normaliza as coordenadas para o intervalo [0, 1].
 */
fun BoundingBox?.normalize(imageWidth: Int, imageHeight: Int): BoundingBox? {
    if (this == null || imageWidth <= 0 || imageHeight <= 0) return null

    return BoundingBox(
        left = (left / imageWidth).coerceIn(0f, 1f),
        top = (top / imageHeight).coerceIn(0f, 1f),
        right = (right / imageWidth).coerceIn(0f, 1f),
        bottom = (bottom / imageHeight).coerceIn(0f, 1f)
    )
}

/**
 * Calcula a interseção com outro bounding box.
 */
fun BoundingBox?.intersect(other: BoundingBox?): BoundingBox? {
    if (this == null || other == null) return null

    val intersectionLeft = maxOf(this.left, other.left)
    val intersectionTop = maxOf(this.top, other.top)
    val intersectionRight = minOf(this.right, other.right)
    val intersectionBottom = minOf(this.bottom, other.bottom)

    return if (intersectionLeft < intersectionRight && intersectionTop < intersectionBottom) {
        BoundingBox(intersectionLeft, intersectionTop, intersectionRight, intersectionBottom)
    } else {
        null
    }
}

/**
 * Calcula a área de sobreposição (IoU - Intersection over Union).
 */
fun BoundingBox?.iou(other: BoundingBox?): Float {
    if (this == null || other == null) return 0f

    val intersection = this.intersect(other) ?: return 0f
    val intersectionArea = intersection.area
    val unionArea = this.area + other.area - intersectionArea

    return if (unionArea > 0) intersectionArea / unionArea else 0f
}

// ========== ARRAY EXTENSIONS ==========

/**
 * Validação mais robusta de dados de imagem.
 * Verifica headers comuns de formatos de imagem.
 */
fun ByteArray?.isValidImageData(): Boolean {
    if (this == null || this.size < 8) return false

    // Verifica headers de formatos comuns
    return when {
        // JPEG
        this.size >= 4 && this[0] == 0xFF.toByte() && this[1] == 0xD8.toByte() -> true
        // PNG
        this.size >= 8 && this[0] == 0x89.toByte() && this[1] == 0x50.toByte() &&
                this[2] == 0x4E.toByte() && this[3] == 0x47.toByte() -> true
        // GIF
        this.size >= 6 && this.sliceArray(0..2).contentEquals("GIF".toByteArray()) -> true
        // WebP
        this.size >= 12 && this.sliceArray(8..11).contentEquals("WEBP".toByteArray()) -> true
        // Fallback: tamanho mínimo heurístico
        this.size > 100 -> true
        else -> false
    }
}

// ========== LIST<FLOAT> EXTENSIONS ==========

/**
 * Encontra o valor mais próximo ao target.
 */
fun List<Float>.closestTo(target: Float): Float? {
    return minByOrNull { abs(it - target) }
}

/**
 * Versão otimizada de filtragem de outliers.
 * Usa apenas uma passagem pelos dados para calcular média e desvio.
 */
fun List<Float>.filterOutliers(stdDevFactor: Float = 2.0f): List<Float> {
    if (size < 3) return this

    // Calcula média e variância em uma única passagem
    var sum = 0f
    var sumSquares = 0f

    forEach { value ->
        sum += value
        sumSquares += value * value
    }

    val mean = sum / size
    val variance = (sumSquares / size) - (mean * mean)
    val stdDev = sqrt(variance)
    val threshold = stdDev * stdDevFactor

    return filter { abs(it - mean) <= threshold }
}

/**
 * Calcula estatísticas básicas da lista.
 */
data class ListStatistics(
    val mean: Float,
    val median: Float,
    val stdDev: Float,
    val min: Float,
    val max: Float
)

fun List<Float>.getStatistics(): ListStatistics? {
    if (isEmpty()) return null

    val sorted = this.sorted()
    val mean = this.average().toFloat()
    val variance = this.map { (it - mean).pow(2) }.average().toFloat()
    val stdDev = sqrt(variance)
    val median = if (sorted.size % 2 == 0) {
        (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2f
    } else {
        sorted[sorted.size / 2]
    }

    return ListStatistics(mean, median, stdDev, sorted.first(), sorted.last())
}

// ========== LIST<DETECTEDOBJECT> EXTENSIONS - NULL SAFE ==========

/**
 * Encontra o objeto com a maior confiança.
 */
fun List<DetectedObject>.getMostConfident(): DetectedObject? {
    return maxByOrNull { it.confidence }
}

/**
 * Filtra por confiança mínima (versão lazy para listas grandes).
 */
fun List<DetectedObject>.filterByConfidence(minConfidence: Float): List<DetectedObject> {
    return filter { it.confidence >= minConfidence }
}

/**
 * Versão sequence para processamento eficiente de listas grandes.
 */
fun List<DetectedObject>.filterByConfidenceSequence(minConfidence: Float): Sequence<DetectedObject> {
    return asSequence().filter { it.confidence >= minConfidence }
}

/**
 * Agrupa por tipo e calcula estatísticas otimizadas.
 */
fun List<DetectedObject>.getTypeStatistics(): Map<ObjectType, TypeStatistics> {
    return groupBy { it.type }.mapValues { (_, objects) ->
        val confidences = objects.map { it.confidence }
        TypeStatistics(
            count = objects.size,
            averageConfidence = confidences.average().toFloat(),
            latestDetection = objects.maxOfOrNull { it.timestamp } ?: 0L
        )
    }
}

/**
 * Filtra objetos duplicados baseado em proximidade de bounding boxes.
 * VERSÃO CORRIGIDA COM NULL SAFETY
 */
fun List<DetectedObject>.removeDuplicates(iouThreshold: Float = 0.5f): List<DetectedObject> {
    if (size <= 1) return this

    val result = mutableListOf<DetectedObject>()
    val processed = mutableSetOf<Int>()

    forEachIndexed { index, obj ->
        if (index in processed) return@forEachIndexed

        result.add(obj)
        processed.add(index)

        // Marca objetos similares como processados - COM NULL SAFETY
        for (i in (index + 1) until size) {
            if (i !in processed) {
                val otherObj = this[i]
                val iou = obj.boundingBox?.iou(otherObj.boundingBox) ?: 0f
                if (iou > iouThreshold) {
                    processed.add(i)
                }
            }
        }
    }

    return result
}

// ========== MATH EXTENSIONS ==========

fun Float.toRadians(): Float = (this * PI / 180).toFloat()
fun Float.toDegrees(): Float = (this * 180 / PI).toFloat()

fun Double.toRadians(): Double = this * PI / 180
fun Double.toDegrees(): Double = this * 180 / PI

/**
 * Interpolação linear entre dois valores.
 */
fun Float.lerp(target: Float, factor: Float): Float {
    return this + (target - this) * factor.coerceIn(0f, 1f)
}

/**
 * Mapeamento de um valor de um intervalo para outro.
 */
fun Float.map(fromMin: Float, fromMax: Float, toMin: Float, toMax: Float): Float {
    if (fromMax - fromMin == 0f) return toMin
    return toMin + (this - fromMin) * (toMax - toMin) / (fromMax - fromMin)
}

// ========== RETRY UTILITIES ==========

/**
 * Função de retry com backoff exponencial.
 * ATENÇÃO: Esta função BLOQUEIA a thread atual.
 * Use apenas em background threads ou com Dispatchers.IO.
 */
inline fun <T> retry(
    times: Int = 3,
    initialDelay: Long = 100,
    maxDelay: Long = 1000,
    factor: Double = 2.0,
    onRetry: (Exception, Int) -> Unit = { _, _ -> },
    block: () -> T
): T {
    require(times > 0) { "Retry times must be positive" }
    require(initialDelay > 0) { "Initial delay must be positive" }

    var currentDelay = initialDelay
    repeat(times) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            if (attempt == times - 1) {
                throw e
            } else {
                onRetry(e, attempt + 1)
                Thread.sleep(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
    }
    error("This should never be reached")
}

/**
 * Versão suspendable (não-bloqueante) do retry.
 * Ideal para uso em corrotinas.
 */
suspend inline fun <T> retrySuspended(
    times: Int = 3,
    initialDelay: Long = 100,
    maxDelay: Long = 1000,
    factor: Double = 2.0,
    onRetry: suspend (Exception, Int) -> Unit = { _, _ -> },
    block: suspend () -> T
): T {
    require(times > 0) { "Retry times must be positive" }
    require(initialDelay > 0) { "Initial delay must be positive" }

    var currentDelay = initialDelay
    repeat(times) { attempt ->
        try {
            return block()
        } catch (e: Exception) {
            if (attempt == times - 1) {
                throw e
            } else {
                onRetry(e, attempt + 1)
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
    }
    error("This should never be reached")
}

// ========== DEBUG EXTENSIONS ==========

/**
 * Log condicional apenas em builds de debug.
 * Agora suporta diferentes níveis de log.
 */
enum class LogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR }

fun Any.logDebug(
    message: String,
    tag: String = "AR_DEBUG",
    level: LogLevel = LogLevel.DEBUG
) {
    if (BuildConfig.DEBUG) {
        val prefix = when (level) {
            LogLevel.VERBOSE -> "V"
            LogLevel.DEBUG -> "D"
            LogLevel.INFO -> "I"
            LogLevel.WARN -> "W"
            LogLevel.ERROR -> "E"
        }
        println("[$prefix/$tag] $message: $this")
    }
}

/**
 * Executa um bloco apenas em builds de debug.
 */
inline fun debugOnly(block: () -> Unit) {
    if (BuildConfig.DEBUG) {
        block()
    }
}

// ========== RANGE EXTENSIONS ==========

fun ClosedFloatingPointRange<Float>.random(): Float {
    return start + (endInclusive - start) * Random.nextFloat()
}

fun ClosedFloatingPointRange<Double>.random(): Double {
    return start + (endInclusive - start) * Random.nextDouble()
}

/**
 * Verifica se um valor está no intervalo.
 */
fun ClosedFloatingPointRange<Float>.contains(value: Double): Boolean {
    return value.toFloat() in this
}

/**
 * Retorna o tamanho do intervalo.
 */
val ClosedFloatingPointRange<Float>.size: Float
    get() = endInclusive - start

val ClosedFloatingPointRange<Double>.size: Double
    get() = endInclusive - start

// ========== COLLECTION UTILITIES ==========

/**
 * Particiona uma lista em chunks de tamanho específico.
 */
fun <T> List<T>.chunked(size: Int): List<List<T>> {
    require(size > 0) { "Chunk size must be positive" }
    return windowed(size, size, true)
}

/**
 * Encontra elementos únicos baseado em uma função de comparação.
 */
inline fun <T, K> List<T>.distinctBy(selector: (T) -> K): List<T> {
    val seen = mutableSetOf<K>()
    return filter { seen.add(selector(it)) }
}

// ========== PERFORMANCE UTILITIES ==========

/**
 * Mede o tempo de execução de um bloco de código.
 */
inline fun <T> measureTimeMillisDebug(tag: String = "TIMING", block: () -> T): T {
    return if (BuildConfig.DEBUG) {
        val startTime = System.currentTimeMillis()
        val result = block()
        val elapsed = System.currentTimeMillis() - startTime
        println("[$tag] Execution time: ${elapsed}ms")
        result
    } else {
        block()
    }
}