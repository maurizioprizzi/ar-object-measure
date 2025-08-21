package com.objectmeasure.ar.domain.repository

import com.objectmeasure.ar.domain.model.*
import com.objectmeasure.ar.core.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

/**
 * Repository interface para operações com objetos detectados - VERSÃO COMPLETA
 *
 * Interface consolidada que define todas as operações necessárias para um
 * sistema AR completo de detecção e medição de objetos.
 *
 * Compatível com ObjectRepositoryImpl e todo o sistema estabelecido.
 *
 * @version 2.2 FINAL
 */
interface ObjectRepository {

    // ========== CORE DETECTION OPERATIONS ==========

    /**
     * Detecta objetos em uma imagem única
     * @param imageData dados da imagem capturada
     * @return Flow com resultado da detecção
     */
    suspend fun detectObjects(imageData: ByteArray): Flow<Result<List<DetectedObject>>>

    /**
     * Stream contínuo de detecção de objetos
     * @return Flow contínuo com detecções em tempo real
     */
    fun detectObjectsStream(): Flow<Result<List<DetectedObject>>>

    /**
     * Para o stream de detecção contínuo
     */
    fun stopDetectionStream()

    // ========== MEASUREMENT OPERATIONS ==========

    /**
     * Mede um objeto específico baseado em suas coordenadas
     * @param boundingBox coordenadas do objeto na imagem
     * @param objectType tipo do objeto para medição específica
     * @return resultado da medição
     */
    suspend fun measureObject(
        boundingBox: BoundingBox,
        objectType: ObjectType
    ): Result<DetectedObject>

    /**
     * Rastreia movimento de um objeto detectado
     * @param objectId ID do objeto a ser rastreado
     * @param newBoundingBox nova posição do objeto
     * @return objeto atualizado com tracking data
     */
    suspend fun trackObject(
        objectId: String,
        newBoundingBox: BoundingBox
    ): Result<DetectedObject>

    // ========== CALIBRATION OPERATIONS ==========

    /**
     * Calibra o sistema usando objeto de referência
     * @param referenceObject objeto com dimensões conhecidas
     * @param realWorldSize tamanho real do objeto
     * @return dados de calibração
     */
    suspend fun calibrateWithReference(
        referenceObject: DetectedObject,
        realWorldSize: Measurement
    ): Result<CalibrationData>

    // ========== HISTORY AND STORAGE ==========

    /**
     * Obtém histórico de objetos detectados
     * @param limit número máximo de objetos a retornar
     * @return lista dos objetos detectados recentemente
     */
    suspend fun getDetectionHistory(limit: Int = 10): Result<List<DetectedObject>>

    /**
     * Obtém histórico filtrado por critérios específicos
     * @param objectTypes tipos de objetos a filtrar (null = todos)
     * @param minConfidence confidence mínima
     * @param since timestamp mínimo (null = sem limite)
     * @param limit número máximo de resultados
     * @return lista filtrada de objetos
     */
    suspend fun getFilteredHistory(
        objectTypes: List<ObjectType>? = null,
        minConfidence: Float = 0.0f,
        since: Long? = null,
        limit: Int = 50
    ): Result<List<DetectedObject>>

    /**
     * Salva um objeto detectado
     * @param detectedObject objeto a ser salvo
     * @return sucesso ou falha da operação
     */
    suspend fun saveDetectedObject(detectedObject: DetectedObject): Result<Unit>

    /**
     * Remove detecções antigas do cache
     * @param olderThan timestamp limite (objetos mais antigos serão removidos)
     * @return sucesso ou falha da operação
     */
    suspend fun clearOldDetections(olderThan: Long): Result<Unit>

    // ========== AR CONFIGURATION ==========

    /**
     * Verifica se AR está disponível no dispositivo
     * @return true se AR está disponível e configurado
     */
    suspend fun isARAvailable(): Boolean

    /**
     * Configura sessão AR com parâmetros específicos
     * @param config configuração da sessão AR
     * @return sucesso ou falha da configuração
     */
    suspend fun setupARSession(config: ARConfig): Result<Unit>

    /**
     * Obtém configuração atual do AR
     * @return configuração ativa do AR
     */
    suspend fun getARConfig(): Result<ARConfig>

    // ========== STATISTICS AND ANALYTICS ==========

    /**
     * Obtém estatísticas de detecção da sessão atual
     * @return estatísticas detalhadas
     */
    suspend fun getDetectionStats(): Result<DetectionStats>
}

// ========== BOUNDING BOX (Versão Consolidada) ==========

/**
 * Representa as coordenadas de um objeto detectado na imagem
 * Versão consolidada compatível com todo o sistema
 */
@Serializable
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
    val confidence: Float = 1.0f
) {
    init {
        require(confidence in 0f..1f) { "Confidence must be between 0.0 and 1.0: $confidence" }
        require(left >= 0f) { "Left coordinate cannot be negative: $left" }
        require(top >= 0f) { "Top coordinate cannot be negative: $top" }
        require(right > left) { "Right ($right) must be greater than left ($left)" }
        require(bottom > top) { "Bottom ($bottom) must be greater than top ($top)" }
    }

    // ========== BASIC CALCULATIONS ==========

    /**
     * Calcula a largura do bounding box
     */
    fun width(): Float = right - left

    /**
     * Calcula a altura do bounding box
     */
    fun height(): Float = bottom - top

    /**
     * Calcula a área do bounding box
     */
    fun area(): Float = width() * height()

    /**
     * Calcula o perímetro do bounding box
     */
    fun perimeter(): Float = 2 * (width() + height())

    /**
     * Calcula a diagonal do bounding box
     */
    fun diagonal(): Float = kotlin.math.sqrt(width() * width() + height() * height())

    /**
     * Retorna o ponto central do bounding box
     */
    fun center(): Pair<Float, Float> = Pair((left + right) / 2f, (top + bottom) / 2f)

    /**
     * Retorna coordenadas do centro como objeto Point
     */
    fun centerPoint(): Point2D = Point2D((left + right) / 2f, (top + bottom) / 2f)

    // ========== VALIDATION ==========

    /**
     * Verifica se o bounding box é válido
     */
    fun isValid(): Boolean {
        return left >= 0f && top >= 0f &&
                right > left && bottom > top &&
                confidence in 0f..1f &&
                left.isFinite() && top.isFinite() &&
                right.isFinite() && bottom.isFinite()
    }

    /**
     * Verifica se as coordenadas estão dentro dos limites da imagem
     */
    fun isWithinBounds(imageWidth: Float, imageHeight: Float): Boolean {
        return left >= 0f && top >= 0f && right <= imageWidth && bottom <= imageHeight
    }

    /**
     * Verifica se o bounding box tem tamanho razoável
     */
    fun hasReasonableSize(minSize: Float = 10f, maxSize: Float = 5000f): Boolean {
        val w = width()
        val h = height()
        return w >= minSize && h >= minSize && w <= maxSize && h <= maxSize
    }

    /**
     * Verifica se é aproximadamente quadrado
     */
    fun isSquare(tolerance: Float = 0.1f): Boolean {
        val w = width()
        val h = height()
        if (w == 0f || h == 0f) return false
        val ratio = kotlin.math.max(w, h) / kotlin.math.min(w, h)
        return ratio <= (1f + tolerance)
    }

    // ========== GEOMETRIC OPERATIONS ==========

    /**
     * Calcula interseção com outro bounding box
     */
    fun intersect(other: BoundingBox): BoundingBox? {
        val intersectionLeft = kotlin.math.max(this.left, other.left)
        val intersectionTop = kotlin.math.max(this.top, other.top)
        val intersectionRight = kotlin.math.min(this.right, other.right)
        val intersectionBottom = kotlin.math.min(this.bottom, other.bottom)

        return if (intersectionLeft < intersectionRight && intersectionTop < intersectionBottom) {
            BoundingBox(
                left = intersectionLeft,
                top = intersectionTop,
                right = intersectionRight,
                bottom = intersectionBottom,
                confidence = kotlin.math.min(this.confidence, other.confidence)
            )
        } else null
    }

    /**
     * Calcula união com outro bounding box
     */
    fun union(other: BoundingBox): BoundingBox {
        return BoundingBox(
            left = kotlin.math.min(this.left, other.left),
            top = kotlin.math.min(this.top, other.top),
            right = kotlin.math.max(this.right, other.right),
            bottom = kotlin.math.max(this.bottom, other.bottom),
            confidence = kotlin.math.max(this.confidence, other.confidence)
        )
    }

    /**
     * Calcula IoU (Intersection over Union) com outro bounding box
     */
    fun iou(other: BoundingBox): Float {
        val intersection = intersect(other) ?: return 0f
        val intersectionArea = intersection.area()
        val unionArea = this.area() + other.area() - intersectionArea

        return if (unionArea > 0f) intersectionArea / unionArea else 0f
    }

    /**
     * Verifica se contém um ponto
     */
    fun contains(x: Float, y: Float): Boolean {
        return x >= left && x <= right && y >= top && y <= bottom
    }

    /**
     * Verifica se contém um ponto
     */
    fun contains(point: Point2D): Boolean = contains(point.x, point.y)

    /**
     * Verifica se contém completamente outro bounding box
     */
    fun contains(other: BoundingBox): Boolean {
        return other.left >= this.left && other.top >= this.top &&
                other.right <= this.right && other.bottom <= this.bottom
    }

    /**
     * Verifica se sobrepõe com outro bounding box
     */
    fun overlaps(other: BoundingBox): Boolean = intersect(other) != null

    // ========== TRANSFORMATIONS ==========

    /**
     * Expande o bounding box por uma margem específica
     */
    fun expand(margin: Float): BoundingBox {
        require(margin >= 0f) { "Margin cannot be negative: $margin" }
        return BoundingBox(
            left = (left - margin).coerceAtLeast(0f),
            top = (top - margin).coerceAtLeast(0f),
            right = right + margin,
            bottom = bottom + margin,
            confidence = confidence
        )
    }

    /**
     * Reduz o bounding box por uma margem específica
     */
    fun shrink(margin: Float): BoundingBox? {
        require(margin >= 0f) { "Margin cannot be negative: $margin" }
        val newLeft = left + margin
        val newTop = top + margin
        val newRight = right - margin
        val newBottom = bottom - margin

        return if (newRight > newLeft && newBottom > newTop) {
            BoundingBox(newLeft, newTop, newRight, newBottom, confidence)
        } else null
    }

    /**
     * Normaliza coordenadas para o range [0, 1]
     */
    fun normalize(imageWidth: Float, imageHeight: Float): BoundingBox {
        require(imageWidth > 0f && imageHeight > 0f) { "Image dimensions must be positive" }
        return BoundingBox(
            left = (left / imageWidth).coerceIn(0f, 1f),
            top = (top / imageHeight).coerceIn(0f, 1f),
            right = (right / imageWidth).coerceIn(0f, 1f),
            bottom = (bottom / imageHeight).coerceIn(0f, 1f),
            confidence = confidence
        )
    }

    /**
     * Desnormaliza coordenadas do range [0, 1] para pixels
     */
    fun denormalize(imageWidth: Float, imageHeight: Float): BoundingBox {
        require(imageWidth > 0f && imageHeight > 0f) { "Image dimensions must be positive" }
        return BoundingBox(
            left = left * imageWidth,
            top = top * imageHeight,
            right = right * imageWidth,
            bottom = bottom * imageHeight,
            confidence = confidence
        )
    }

    /**
     * Translada o bounding box por um offset
     */
    fun translate(deltaX: Float, deltaY: Float): BoundingBox {
        return BoundingBox(
            left = left + deltaX,
            top = top + deltaY,
            right = right + deltaX,
            bottom = bottom + deltaY,
            confidence = confidence
        )
    }

    /**
     * Escala o bounding box por um fator
     */
    fun scale(factor: Float): BoundingBox {
        require(factor > 0f) { "Scale factor must be positive: $factor" }
        val center = center()
        val newWidth = width() * factor
        val newHeight = height() * factor

        return BoundingBox(
            left = center.first - newWidth / 2f,
            top = center.second - newHeight / 2f,
            right = center.first + newWidth / 2f,
            bottom = center.second + newHeight / 2f,
            confidence = confidence
        )
    }

    // ========== COMPARISON ==========

    /**
     * Calcula distância entre centros de dois bounding boxes
     */
    fun distanceTo(other: BoundingBox): Float {
        val thisCenter = center()
        val otherCenter = other.center()
        val dx = thisCenter.first - otherCenter.first
        val dy = thisCenter.second - otherCenter.second
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    /**
     * Verifica se é similar a outro bounding box
     */
    fun isSimilarTo(
        other: BoundingBox,
        positionTolerance: Float = 10f,
        sizeTolerance: Float = 0.2f
    ): Boolean {
        val distance = distanceTo(other)
        if (distance > positionTolerance) return false

        val sizeDiff = kotlin.math.abs(area() - other.area()) / kotlin.math.max(area(), other.area())
        return sizeDiff <= sizeTolerance
    }

    // ========== FORMATTING ==========

    /**
     * Retorna representação string formatada
     */
    fun formatCoordinates(precision: Int = 1): String {
        return "(${"%.${precision}f".format(left)}, ${"%.${precision}f".format(top)}) - " +
                "(${"%.${precision}f".format(right)}, ${"%.${precision}f".format(bottom)})"
    }

    /**
     * Retorna dimensões formatadas
     */
    fun formatDimensions(precision: Int = 1): String {
        return "${"%.${precision}f".format(width())} x ${"%.${precision}f".format(height())}"
    }

    /**
     * Resumo compacto
     */
    fun getSummary(): String {
        return "${formatDimensions()} @ ${formatCoordinates()} (${(confidence * 100).roundToInt()}%)"
    }

    companion object {
        /**
         * Cria bounding box a partir do centro e dimensões
         */
        fun fromCenter(
            centerX: Float,
            centerY: Float,
            width: Float,
            height: Float,
            confidence: Float = 1.0f
        ): BoundingBox {
            val halfWidth = width / 2f
            val halfHeight = height / 2f
            return BoundingBox(
                left = centerX - halfWidth,
                top = centerY - halfHeight,
                right = centerX + halfWidth,
                bottom = centerY + halfHeight,
                confidence = confidence
            )
        }

        /**
         * Cria bounding box a partir de dois pontos
         */
        fun fromPoints(point1: Point2D, point2: Point2D, confidence: Float = 1.0f): BoundingBox {
            return BoundingBox(
                left = kotlin.math.min(point1.x, point2.x),
                top = kotlin.math.min(point1.y, point2.y),
                right = kotlin.math.max(point1.x, point2.x),
                bottom = kotlin.math.max(point1.y, point2.y),
                confidence = confidence
            )
        }

        /**
         * Cria bounding box que engloba uma lista de pontos
         */
        fun fromPoints(points: List<Point2D>, confidence: Float = 1.0f): BoundingBox? {
            if (points.isEmpty()) return null

            val minX = points.minOf { it.x }
            val minY = points.minOf { it.y }
            val maxX = points.maxOf { it.x }
            val maxY = points.maxOf { it.y }

            return BoundingBox(minX, minY, maxX, maxY, confidence)
        }

        /**
         * Cria bounding box mock para testes
         */
        fun createMock(
            width: Float = 100f,
            height: Float = 150f,
            x: Float = 200f,
            y: Float = 300f,
            confidence: Float = 0.8f
        ): BoundingBox {
            return BoundingBox(
                left = x,
                top = y,
                right = x + width,
                bottom = y + height,
                confidence = confidence
            )
        }

        /**
         * Cria bounding box aleatório dentro de limites
         */
        fun createRandom(
            imageWidth: Float,
            imageHeight: Float,
            minSize: Float = 50f,
            maxSize: Float = 200f
        ): BoundingBox {
            val width = kotlin.random.Random.nextFloat() * (maxSize - minSize) + minSize
            val height = kotlin.random.Random.nextFloat() * (maxSize - minSize) + minSize
            val x = kotlin.random.Random.nextFloat() * (imageWidth - width)
            val y = kotlin.random.Random.nextFloat() * (imageHeight - height)
            val confidence = kotlin.random.Random.nextFloat() * 0.4f + 0.6f // 0.6-1.0

            return BoundingBox(x, y, x + width, y + height, confidence)
        }
    }
}

// ========== SUPPORT DATA CLASSES ==========

/**
 * Representa um ponto 2D
 */
@Serializable
data class Point2D(val x: Float, val y: Float) {
    fun distanceTo(other: Point2D): Float {
        val dx = x - other.x
        val dy = y - other.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    fun isValid(): Boolean = x.isFinite() && y.isFinite()
}

/**
 * Configuração AR simplificada (compatível com DI module)
 */
data class ARConfig(
    val enableCloudAnchors: Boolean = false,
    val enableLightEstimation: Boolean = true,
    val preferredFPS: Int = 30,
    val autoFocus: Boolean = true,
    val imageStabilization: Boolean = true,
    val planeFindingMode: PlaneFindingMode = PlaneFindingMode.HORIZONTAL_AND_VERTICAL,
    val lightEstimationMode: LightEstimationMode = LightEstimationMode.ENVIRONMENTAL_HDR,
    val cameraConfig: CameraConfig = CameraConfig.default()
) {
    fun isValid(): Boolean {
        return preferredFPS in 1..60 &&
                planeFindingMode != null &&
                lightEstimationMode != null
    }
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

data class CameraConfig(
    val resolution: String = "1920x1080",
    val enableHDR: Boolean = false
) {
    companion object {
        fun default() = CameraConfig()
    }
}

/**
 * Dados de calibração
 */
@Serializable
data class CalibrationData(
    val pixelsPerMeter: Float,
    val distanceScale: Float = 1.0f,
    val referenceObjectType: ObjectType,
    val referenceSize: Measurement,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val validUntil: Long = System.currentTimeMillis() + 30 * 60 * 1000L // 30 min
) {
    fun isValid(): Boolean = pixelsPerMeter > 0 && confidence in 0f..1f
    fun isExpired(): Boolean = System.currentTimeMillis() > validUntil
}

/**
 * Estatísticas de detecção
 */
@Serializable
data class DetectionStats(
    val totalDetections: Int,
    val reliableDetections: Int,
    val averageConfidence: Float,
    val mostDetectedType: ObjectType?,
    val sessionDuration: Long,
    val frameCount: Long = 0L,
    val averageFPS: Float = 0f,
    val cacheUtilization: Float = 0f,
    val memoryUsage: Long = 0L,
    val calibrationStatus: CalibrationStatus = CalibrationStatus.NONE
)

enum class CalibrationStatus {
    NONE, ACTIVE, EXPIRED, INVALID
}

// ========== EXTENSION FUNCTIONS ==========

/**
 * Extensões para listas de BoundingBox
 */
fun List<BoundingBox>.filterValid(): List<BoundingBox> = filter { it.isValid() }

fun List<BoundingBox>.filterByConfidence(minConfidence: Float): List<BoundingBox> =
    filter { it.confidence >= minConfidence }

fun List<BoundingBox>.getBoundingBoxForAll(): BoundingBox? {
    if (isEmpty()) return null

    val minLeft = minOf { it.left }
    val minTop = minOf { it.top }
    val maxRight = maxOf { it.right }
    val maxBottom = maxOf { it.bottom }
    val avgConfidence = map { it.confidence }.average().toFloat()

    return BoundingBox(minLeft, minTop, maxRight, maxBottom, avgConfidence)
}

fun List<BoundingBox>.removeOverlapping(iouThreshold: Float = 0.5f): List<BoundingBox> {
    if (size <= 1) return this

    val result = mutableListOf<BoundingBox>()
    val processed = BooleanArray(size)

    forEachIndexed { index, bbox ->
        if (processed[index]) return@forEachIndexed

        result.add(bbox)
        processed[index] = true

        for (i in (index + 1) until size) {
            if (!processed[i] && bbox.iou(this[i]) > iouThreshold) {
                processed[i] = true
            }
        }
    }

    return result
}