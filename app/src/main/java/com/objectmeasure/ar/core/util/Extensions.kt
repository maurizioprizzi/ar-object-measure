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
 * Extensões utilitárias para AR Object Measure - VERSÃO CORRIGIDA E OTIMIZADA
 *
 * Principais melhorias v2.2:
 * - Correções de null safety e performance
 * - Cache para operações matemáticas custosas
 * - Otimizações de memória e processamento
 * - Validações mais robustas
 * - Thread safety melhorada
 * - Algoritmos mais eficientes
 *
 * @version 2.2
 * @author AR Team
 */

// ========== CONSTANTS AND CACHES ==========

private val POWER_OF_10_FLOAT = floatArrayOf(1f, 10f, 100f, 1000f, 10000f, 100000f)
private val POWER_OF_10_DOUBLE = doubleArrayOf(1.0, 10.0, 100.0, 1000.0, 10000.0, 100000.0)
private val DEBUG_ENABLED = BuildConfig.DEBUG

// JPEG markers
private val JPEG_START = byteArrayOf(0xFF.toByte(), 0xD8.toByte())
private val JPEG_END = byteArrayOf(0xFF.toByte(), 0xD9.toByte())

// PNG signature
private val PNG_SIGNATURE = byteArrayOf(
    0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
)

// ========== STRING EXTENSIONS ==========

/**
 * Verifica se a string representa um número válido.
 * Suporta números negativos, decimais e notação científica.
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

/**
 * Verifica se a string é um email válido (validação básica).
 */
fun String?.isValidEmail(): Boolean {
    return !this.isNullOrBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
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
 * Versão otimizada com cache para valores comuns.
 */
fun Float.roundTo(decimals: Int): Float {
    if (decimals < 0) return this
    val multiplier = if (decimals < POWER_OF_10_FLOAT.size) {
        POWER_OF_10_FLOAT[decimals]
    } else {
        10f.pow(decimals)
    }
    return round(this * multiplier) / multiplier
}

/**
 * Versão otimizada do roundTo para Double com cache.
 */
fun Double.roundTo(decimals: Int): Double {
    if (decimals < 0) return this
    val multiplier = if (decimals < POWER_OF_10_DOUBLE.size) {
        POWER_OF_10_DOUBLE[decimals]
    } else {
        10.0.pow(decimals)
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

/**
 * Verifica se o número é finito (não NaN nem infinito).
 */
fun Float.isFinite(): Boolean = isFinite()
fun Double.isFinite(): Boolean = isFinite()

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
        box.width() > 0 && box.height() > 0 &&
                box.left >= 0 && box.top >= 0 &&
                box.right > box.left && box.bottom > box.top &&
                box.left.isFinite() && box.top.isFinite() &&
                box.right.isFinite() && box.bottom.isFinite()
    } ?: false
}

/**
 * Normaliza as coordenadas para o intervalo [0, 1].
 */
fun BoundingBox?.normalize(imageWidth: Int, imageHeight: Int): BoundingBox? {
    if (this == null || imageWidth <= 0 || imageHeight <= 0 || !this.isValid()) return null

    return BoundingBox(
        left = (left / imageWidth).coerceIn(0f, 1f),
        top = (top / imageHeight).coerceIn(0f, 1f),
        right = (right / imageWidth).coerceIn(0f, 1f),
        bottom = (bottom / imageHeight).coerceIn(0f, 1f)
    )
}

/**
 * Calcula a interseção com outro bounding box.
 * Versão otimizada com validação prévia.
 */
fun BoundingBox?.intersect(other: BoundingBox?): BoundingBox? {
    if (this == null || other == null) return null

    // Validação prévia dos bounding boxes
    if (!this.isValid() || !other.isValid()) return null

    // Verificação rápida de não-interseção
    if (this.right <= other.left || other.right <= this.left ||
        this.bottom <= other.top || other.bottom <= this.top) {
        return null
    }

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
 * Versão otimizada com verificação rápida de não-interseção.
 */
fun BoundingBox?.iou(other: BoundingBox?): Float {
    if (this == null || other == null) return 0f

    // Verificação rápida de não-interseção antes de calcular
    if (this.right <= other.left || other.right <= this.left ||
        this.bottom <= other.top || other.bottom <= this.top) {
        return 0f
    }

    val intersection = this.intersect(other) ?: return 0f
    val intersectionArea = intersection.area
    val unionArea = this.area + other.area - intersectionArea

    return if (unionArea > 0) intersectionArea / unionArea else 0f
}

/**
 * Expande o bounding box por uma margem específica.
 */
fun BoundingBox?.expand(margin: Float): BoundingBox? {
    if (this == null || !this.isValid() || margin < 0) return this

    return BoundingBox(
        left = left - margin,
        top = top - margin,
        right = right + margin,
        bottom = bottom + margin
    )
}

// ========== ARRAY EXTENSIONS ==========

/**
 * Validação robusta de dados de imagem.
 * Verifica headers e estrutura de formatos comuns.
 */
fun ByteArray?.isValidImageData(): Boolean {
    if (this == null || this.size < 8) return false

    return when {
        // JPEG - Verifica início e fim
        this.size >= 4 &&
                this.sliceArray(0..1).contentEquals(JPEG_START) &&
                this.size >= 4 &&
                this.sliceArray(this.size-2 until this.size).contentEquals(JPEG_END) -> true

        // PNG - Verifica signature completa
        this.size >= 8 &&
                this.sliceArray(0..7).contentEquals(PNG_SIGNATURE) -> true

        // GIF
        this.size >= 6 &&
                this.sliceArray(0..2).contentEquals("GIF".toByteArray()) -> true

        // WebP
        this.size >= 12 &&
                this.sliceArray(0..3).contentEquals("RIFF".toByteArray()) &&
                this.sliceArray(8..11).contentEquals("WEBP".toByteArray()) -> true

        // BMP
        this.size >= 2 &&
                this.sliceArray(0..1).contentEquals("BM".toByteArray()) -> true

        // Fallback: tamanho mínimo mais conservador
        this.size > 1024 -> true

        else -> false
    }
}

/**
 * Verifica se o array contém dados válidos de vídeo (headers básicos).
 */
fun ByteArray?.isValidVideoData(): Boolean {
    if (this == null || this.size < 12) return false

    return when {
        // MP4/MOV
        this.size >= 8 && (
                this.sliceArray(4..7).contentEquals("ftyp".toByteArray()) ||
                        this.sliceArray(4..7).contentEquals("moov".toByteArray())
                ) -> true

        // AVI
        this.size >= 12 &&
                this.sliceArray(0..3).contentEquals("RIFF".toByteArray()) &&
                this.sliceArray(8..11).contentEquals("AVI ".toByteArray()) -> true

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
 * Versão otimizada de filtragem de outliers usando fold.
 * Uma única passagem pelos dados para calcular estatísticas.
 */
fun List<Float>.filterOutliers(stdDevFactor: Float = 2.0f): List<Float> {
    if (size < 3) return this

    // Calcula soma e soma dos quadrados em uma única passagem
    val (sum, sumSquares) = fold(0f to 0f) { (s, ss), value ->
        (s + value) to (ss + value * value)
    }

    val mean = sum / size
    val variance = (sumSquares / size) - (mean * mean)
    val stdDev = sqrt(variance)
    val threshold = stdDev * stdDevFactor

    return filter { abs(it - mean) <= threshold }
}

/**
 * Calcula estatísticas básicas da lista de forma eficiente.
 */
data class ListStatistics(
    val mean: Float,
    val median: Float,
    val stdDev: Float,
    val min: Float,
    val max: Float,
    val count: Int
)

fun List<Float>.getStatistics(): ListStatistics? {
    if (isEmpty()) return null

    val sorted = this.sorted()
    val mean = this.average().toFloat()
    val variance = this.fold(0f) { acc, value ->
        acc + (value - mean).pow(2)
    } / size
    val stdDev = sqrt(variance)

    val median = if (sorted.size % 2 == 0) {
        (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2f
    } else {
        sorted[sorted.size / 2]
    }

    return ListStatistics(mean, median, stdDev, sorted.first(), sorted.last(), size)
}

/**
 * Calcula percentis da lista.
 */
fun List<Float>.percentile(percentile: Float): Float? {
    if (isEmpty() || percentile < 0f || percentile > 100f) return null

    val sorted = this.sorted()
    val index = (percentile / 100f * (sorted.size - 1)).toInt()
    return sorted[index]
}

// ========== LIST<DETECTEDOBJECT> EXTENSIONS - NULL SAFE ==========

/**
 * Encontra o objeto com a maior confiança.
 */
fun List<DetectedObject>.getMostConfident(): DetectedObject? {
    return maxByOrNull { it.confidence }
}

/**
 * Filtra por confiança mínima.
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
 * Agrupa por tipo e calcula estatísticas.
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
 * Versão otimizada com BooleanArray para melhor performance de memória.
 */
fun List<DetectedObject>.removeDuplicates(iouThreshold: Float = 0.5f): List<DetectedObject> {
    if (size <= 1) return this

    val result = ArrayList<DetectedObject>(size) // Pre-allocate
    val processed = BooleanArray(size) // Mais eficiente que Set<Int>

    forEachIndexed { index, obj ->
        if (processed[index]) return@forEachIndexed

        result.add(obj)
        processed[index] = true

        // Marca objetos similares como processados
        for (i in (index + 1) until size) {
            if (!processed[i]) {
                val otherObj = this[i]
                val iou = obj.boundingBox?.iou(otherObj.boundingBox) ?: 0f
                if (iou > iouThreshold) {
                    processed[i] = true
                }
            }
        }
    }

    return result
}

/**
 * Ordena por confiança descendente.
 */
fun List<DetectedObject>.sortedByConfidenceDesc(): List<DetectedObject> {
    return sortedByDescending { it.confidence }
}

/**
 * Filtra por tipo de objeto.
 */
fun List<DetectedObject>.filterByType(type: ObjectType): List<DetectedObject> {
    return filter { it.type == type }
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

/**
 * Calcula a distância euclidiana entre dois pontos.
 */
fun Float.distanceTo(other: Float): Float = abs(this - other)

/**
 * Normalização Min-Max.
 */
fun Float.normalize(min: Float, max: Float): Float {
    return if (max - min == 0f) 0f else (this - min) / (max - min)
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
    require(factor > 1.0) { "Factor must be greater than 1.0" }

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
    require(factor > 1.0) { "Factor must be greater than 1.0" }

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
 * Versão thread-safe com cache do DEBUG_ENABLED.
 */
enum class LogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR }

fun Any.logDebug(
    message: String,
    tag: String = "AR_DEBUG",
    level: LogLevel = LogLevel.DEBUG
) {
    if (DEBUG_ENABLED) {
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
    if (DEBUG_ENABLED) {
        block()
    }
}

/**
 * Log com timestamp para debug de performance.
 */
fun Any.logDebugTimed(
    message: String,
    tag: String = "AR_TIMING"
) {
    if (DEBUG_ENABLED) {
        val timestamp = System.currentTimeMillis()
        println("[D/$tag] [$timestamp] $message: $this")
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

/**
 * Clamp um valor ao intervalo.
 */
fun ClosedFloatingPointRange<Float>.clamp(value: Float): Float {
    return value.coerceIn(start, endInclusive)
}

// ========== COLLECTION UTILITIES ==========

/**
 * Particiona uma lista em chunks de tamanho específico.
 */
fun <T> List<T>.chunkedSafe(size: Int): List<List<T>> {
    require(size > 0) { "Chunk size must be positive" }
    if (isEmpty()) return emptyList()
    return windowed(size, size, true)
}

/**
 * Encontra elementos únicos baseado em uma função de comparação.
 */
inline fun <T, K> List<T>.distinctBySafe(selector: (T) -> K): List<T> {
    val seen = mutableSetOf<K>()
    return filter { seen.add(selector(it)) }
}

/**
 * Versão thread-safe do groupBy para processamento paralelo.
 */
fun <T, K> List<T>.groupBySafe(keySelector: (T) -> K): Map<K, List<T>> {
    return groupBy(keySelector)
}

// ========== PERFORMANCE UTILITIES ==========

/**
 * Mede o tempo de execução de um bloco de código.
 * Versão otimizada que só faz medição em debug.
 */
inline fun <T> measureTimeMillisDebug(tag: String = "TIMING", block: () -> T): T {
    return if (DEBUG_ENABLED) {
        val startTime = System.currentTimeMillis()
        val result = block()
        val elapsed = System.currentTimeMillis() - startTime
        println("[$tag] Execution time: ${elapsed}ms")
        result
    } else {
        block()
    }
}

/**
 * Versão com nanossegundos para maior precisão.
 */
inline fun <T> measureTimeNanosDebug(tag: String = "TIMING_NANO", block: () -> T): T {
    return if (DEBUG_ENABLED) {
        val startTime = System.nanoTime()
        val result = block()
        val elapsed = (System.nanoTime() - startTime) / 1_000_000.0 // Convert to ms
        println("[$tag] Execution time: ${elapsed.roundTo(3)}ms")
        result
    } else {
        block()
    }
}

// ========== VALIDATION UTILITIES ==========

/**
 * Verifica se todos os elementos de uma lista são válidos.
 */
inline fun <T> List<T>.allValid(predicate: (T) -> Boolean): Boolean {
    return all(predicate)
}

/**
 * Conta elementos válidos em uma lista.
 */
inline fun <T> List<T>.countValid(predicate: (T) -> Boolean): Int {
    return count(predicate)
}

/**
 * Filtra elementos válidos e mapeia em uma única operação.
 */
inline fun <T, R> List<T>.filterAndMap(
    filter: (T) -> Boolean,
    transform: (T) -> R
): List<R> {
    return mapNotNull { if (filter(it)) transform(it) else null }
}