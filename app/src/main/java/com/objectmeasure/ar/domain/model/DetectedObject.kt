package com.objectmeasure.ar.domain.model

import com.objectmeasure.ar.core.util.*
import com.objectmeasure.ar.domain.repository.BoundingBox
import kotlinx.serialization.Serializable
import java.util.UUID
import java.util.Locale
import kotlin.math.*

/**
 * Representa um objeto detectado pela câmera AR - VERSÃO COMPLETA E OTIMIZADA
 *
 * Melhorias implementadas:
 * - Classes de suporte completas e validadas
 * - Performance otimizada com cache de cálculos
 * - API consistente e thread-safe
 * - Validações eficientes
 * - Factory methods robustos
 * - Sistema de medições completo
 * - Tracking data integrado
 * - Export/import funcional
 *
 * @version 2.2
 */
@Serializable
data class DetectedObject(
    val id: String = UUID.randomUUID().toString(),
    val type: ObjectType,
    val measurements: ObjectMeasurements,
    val confidence: Float,
    val position: Position3D? = null,
    val boundingBox: BoundingBox? = null,
    val trackingState: TrackingState = TrackingState.TRACKING,
    val trackingData: TrackingData? = null,
    val lastUpdated: Long = getCurrentTimeMillis(),
    val timestamp: Long = getCurrentTimeMillis(),
    val isStatic: Boolean = false,
    val sessionId: String? = null,
    private val _cachedValidation: Boolean? = null // Cache de validação
) {
    init {
        // Validações otimizadas - apenas as essenciais
        require(confidence in 0f..1f) { "Confidence must be between 0.0 and 1.0: $confidence" }
        require(id.isNotBlank()) { "ID cannot be blank" }
    }

    // ========== CACHED PROPERTIES ==========

    /**
     * Validation cache para evitar recálculos
     */
    private val isValidCached: Boolean by lazy {
        confidence.isValidConfidence() &&
                measurements.isValid() &&
                (boundingBox?.isValid() != false) &&
                (position?.isValid() != false) &&
                id.isNotBlank()
    }

    /**
     * Display name cache
     */
    private val displayNameCached: String by lazy {
        type.displayName
    }

    /**
     * Age cache (recalculado conforme necessário)
     */
    val ageMs: Long get() = getCurrentTimeMillis() - timestamp
    val lastUpdateAgeMs: Long get() = getCurrentTimeMillis() - lastUpdated

    // ========== CONFIDENCE VALIDATION ==========

    fun isReliableDetection(threshold: Float = DEFAULT_CONFIDENCE_THRESHOLD): Boolean {
        return confidence >= threshold && confidence.isValidConfidence()
    }

    fun isHighConfidenceDetection(): Boolean {
        return confidence >= HIGH_CONFIDENCE_THRESHOLD
    }

    fun withNormalizedConfidence(): DetectedObject {
        val normalizedConfidence = confidence.coerceIn(0f, 1f)
        return if (normalizedConfidence == confidence) this else copy(confidence = normalizedConfidence)
    }

    fun getConfidenceLevel(): ConfidenceLevel {
        return when {
            confidence < 0.3f -> ConfidenceLevel.LOW
            confidence < 0.6f -> ConfidenceLevel.MEDIUM
            confidence < 0.9f -> ConfidenceLevel.HIGH
            else -> ConfidenceLevel.VERY_HIGH
        }
    }

    // ========== MEASUREMENT VALIDATION ==========

    fun hasUsefulMeasurements(): Boolean = measurements.hasAnyMeasurement()

    fun hasCompleteMeasurements(): Boolean = measurements.hasAllMeasurements()

    fun hasReliableMeasurements(threshold: Float = 0.7f): Boolean {
        return measurements.getAllMeasurements().all { it.isReliable(threshold) }
    }

    fun getPrimaryMeasurement(): Measurement? = measurements.getPrimaryMeasurement()

    fun getMeasurementByType(type: MeasurementType): Measurement? {
        return measurements.getMeasurementByType(type)
    }

    // ========== POSITION AND TRACKING ==========

    fun hasPosition(): Boolean = position != null

    fun hasValidPosition(): Boolean = position?.isValid() == true

    fun isProperlyTracked(): Boolean {
        return trackingState == TrackingState.TRACKING && lastUpdateAgeMs < TRACKING_TIMEOUT_MS
    }

    fun isTrackingLost(): Boolean {
        return trackingState == TrackingState.LOST || lastUpdateAgeMs > TRACKING_TIMEOUT_MS
    }

    fun distanceTo(other: DetectedObject): Float? {
        return if (position != null && other.position != null) {
            position.distanceTo(other.position)
        } else null
    }

    fun distanceTo(point: Position3D): Float? = position?.distanceTo(point)

    fun isNearTo(other: DetectedObject, maxDistance: Float): Boolean {
        val distance = distanceTo(other)
        return distance != null && distance <= maxDistance
    }

    fun getVelocity(): Float? = trackingData?.velocity

    fun getDirection(): Float? = trackingData?.direction

    // ========== OBJECT UPDATES ==========

    fun updateMeasurements(newMeasurements: ObjectMeasurements): DetectedObject {
        return copy(
            measurements = newMeasurements,
            lastUpdated = getCurrentTimeMillis()
        )
    }

    fun updatePosition(
        newPosition: Position3D,
        newTrackingState: TrackingState = TrackingState.TRACKING
    ): DetectedObject {
        return copy(
            position = newPosition,
            trackingState = newTrackingState,
            lastUpdated = getCurrentTimeMillis(),
            trackingData = trackingData?.updatePosition(newPosition, getCurrentTimeMillis())
        )
    }

    fun updateBoundingBox(newBoundingBox: BoundingBox): DetectedObject {
        return copy(
            boundingBox = newBoundingBox,
            lastUpdated = getCurrentTimeMillis()
        )
    }

    fun updateConfidence(newConfidence: Float): DetectedObject {
        require(newConfidence in 0f..1f) { "Invalid confidence: $newConfidence" }
        return copy(
            confidence = newConfidence,
            lastUpdated = getCurrentTimeMillis()
        )
    }

    fun updateTrackingData(newTrackingData: TrackingData): DetectedObject {
        return copy(
            trackingData = newTrackingData,
            lastUpdated = getCurrentTimeMillis()
        )
    }

    fun markAsLost(): DetectedObject {
        return copy(
            trackingState = TrackingState.LOST,
            lastUpdated = getCurrentTimeMillis()
        )
    }

    fun markAsStatic(isStatic: Boolean = true): DetectedObject {
        return copy(isStatic = isStatic, lastUpdated = getCurrentTimeMillis())
    }

    // ========== DISPLAY AND FORMATTING ==========

    fun getDisplayName(): String = displayNameCached

    fun getDetailedDescription(): String {
        val desc = StringBuilder(getDisplayName())

        getPrimaryMeasurement()?.let { primaryMeasurement ->
            desc.append(" (${primaryMeasurement.formatDisplay()})")
        }

        position?.let { pos ->
            desc.append(" - ${pos.z.roundTo(2)}m de distância")
        }

        desc.append(" - ${getConfidencePercentage()} confiança")

        if (trackingState != TrackingState.TRACKING) {
            desc.append(" [${trackingState.displayName}]")
        }

        return desc.toString()
    }

    fun getConfidencePercentage(): String = "${(confidence * 100).roundTo(1)}%"

    fun getFormattedAge(): String {
        val ageSeconds = ageMs / 1000
        return when {
            ageSeconds < 60 -> "${ageSeconds}s"
            ageSeconds < 3600 -> "${ageSeconds / 60}m"
            else -> "${ageSeconds / 3600}h"
        }
    }

    fun getSummary(): ObjectSummary {
        return ObjectSummary(
            id = id,
            type = type,
            displayName = getDisplayName(),
            confidence = confidence,
            confidenceLevel = getConfidenceLevel(),
            hasPosition = hasPosition(),
            hasMeasurements = hasUsefulMeasurements(),
            age = getFormattedAge(),
            trackingState = trackingState
        )
    }

    // ========== VALIDATION ==========

    fun isValid(): Boolean = isValidCached

    fun isRecent(maxAgeMs: Long = 30_000): Boolean = ageMs <= maxAgeMs

    fun isRecentlyUpdated(maxAgeMs: Long = 5_000): Boolean = lastUpdateAgeMs <= maxAgeMs

    fun isOld(maxAgeMs: Long = 300_000): Boolean = ageMs > maxAgeMs // 5 minutos

    fun needsUpdate(): Boolean = lastUpdateAgeMs > UPDATE_THRESHOLD_MS

    fun canBeTracked(): Boolean {
        return hasValidPosition() && confidence >= MIN_TRACKING_CONFIDENCE && !isStatic
    }

    // ========== COMPARISON AND SIMILARITY ==========

    fun isSimilarTo(
        other: DetectedObject,
        positionTolerance: Float = 0.1f,
        timeTolerance: Long = 1000,
        confidenceTolerance: Float = 0.2f
    ): Boolean {
        if (type != other.type) return false
        if (abs(timestamp - other.timestamp) > timeTolerance) return false
        if (abs(confidence - other.confidence) > confidenceTolerance) return false

        val distance = distanceTo(other)
        if (distance != null && distance > positionTolerance) return false

        return true
    }

    fun similarityScore(other: DetectedObject): Float {
        if (type != other.type) return 0f

        var score = 0f
        var factors = 0

        // Confidence similarity (peso 0.3)
        val confidenceDiff = abs(confidence - other.confidence)
        score += (1f - confidenceDiff) * 0.3f
        factors++

        // Position similarity (peso 0.4)
        distanceTo(other)?.let { distance ->
            score += max(0f, 1f - distance / 2f) * 0.4f // 2m max distance
            factors++
        }

        // Time similarity (peso 0.2)
        val timeDiff = abs(timestamp - other.timestamp).toFloat()
        score += max(0f, 1f - timeDiff / 5000f) * 0.2f // 5s max time
        factors++

        // Type exact match bonus (peso 0.1)
        score += 0.1f
        factors++

        return score.coerceIn(0f, 1f)
    }

    fun getMatchScore(criteria: ObjectMatchCriteria): Float {
        var score = 0f
        var maxScore = 0f

        criteria.requiredType?.let { requiredType ->
            maxScore += 1f
            if (type == requiredType) score += 1f
        }

        criteria.minConfidence?.let { minConf ->
            maxScore += 1f
            if (confidence >= minConf) score += 1f
        }

        criteria.maxAge?.let { maxAge ->
            maxScore += 1f
            if (ageMs <= maxAge) score += 1f
        }

        criteria.requiredPosition?.let { reqPos ->
            maxScore += 1f
            position?.let { pos ->
                val distance = pos.distanceTo(reqPos)
                if (distance <= criteria.positionTolerance) score += 1f
            }
        }

        return if (maxScore > 0) score / maxScore else 1f
    }

    // ========== CONVERSION AND EXPORT ==========

    fun convertMeasurements(preferences: UserMeasurementPreferences): DetectedObject {
        return copy(measurements = measurements.convertTo(preferences))
    }

    fun toExportData(): ObjectExportData {
        return ObjectExportData(
            id = id,
            type = type.name,
            displayName = getDisplayName(),
            measurements = measurements.toExportData(),
            confidence = confidence,
            confidenceLevel = getConfidenceLevel().name,
            position = position,
            boundingBox = boundingBox?.let { bbox ->
                mapOf(
                    "left" to bbox.left.toString(),
                    "top" to bbox.top.toString(),
                    "right" to bbox.right.toString(),
                    "bottom" to bbox.bottom.toString()
                )
            },
            timestamp = timestamp,
            lastUpdated = lastUpdated,
            trackingState = trackingState.name,
            isStatic = isStatic,
            sessionId = sessionId,
            detailedDescription = getDetailedDescription(),
            summary = getSummary()
        )
    }

    fun toMinimalData(): ObjectMinimalData {
        return ObjectMinimalData(
            id = id,
            type = type,
            confidence = confidence,
            position = position,
            timestamp = timestamp
        )
    }

    // ========== QUALITY ASSESSMENT ==========

    fun getQualityScore(): Float {
        var score = 0f
        var maxScore = 0f

        // Confidence quality (peso 0.4)
        maxScore += 0.4f
        score += confidence * 0.4f

        // Measurement quality (peso 0.3)
        maxScore += 0.3f
        if (hasUsefulMeasurements()) {
            val measurementQuality = measurements.getAverageConfidence()
            score += measurementQuality * 0.3f
        }

        // Position quality (peso 0.2)
        maxScore += 0.2f
        if (hasValidPosition()) {
            score += 0.2f
        }

        // Tracking quality (peso 0.1)
        maxScore += 0.1f
        if (isProperlyTracked()) {
            score += 0.1f
        }

        return if (maxScore > 0) score / maxScore else 0f
    }

    fun getQualityLevel(): QualityLevel {
        val score = getQualityScore()
        return when {
            score >= 0.9f -> QualityLevel.EXCELLENT
            score >= 0.75f -> QualityLevel.GOOD
            score >= 0.6f -> QualityLevel.FAIR
            score >= 0.4f -> QualityLevel.POOR
            else -> QualityLevel.VERY_POOR
        }
    }

    companion object {
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.7f
        const val HIGH_CONFIDENCE_THRESHOLD = 0.85f
        const val MIN_TRACKING_CONFIDENCE = 0.6f
        const val TRACKING_TIMEOUT_MS = 2000L
        const val UPDATE_THRESHOLD_MS = 1000L

        private fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

        // ========== FACTORY METHODS ==========

        fun createMock(
            type: ObjectType = ObjectType.PHONE,
            confidence: Float = 0.8f,
            withPosition: Boolean = true,
            withMeasurements: Boolean = true,
            withTracking: Boolean = false
        ): DetectedObject {
            return DetectedObject(
                type = type,
                measurements = if (withMeasurements) {
                    ObjectMeasurements.createMockForType(type)
                } else {
                    ObjectMeasurements.empty()
                },
                confidence = confidence.coerceIn(0f, 1f),
                position = if (withPosition) Position3D.createMock() else null,
                boundingBox = BoundingBox.createMock(),
                trackingData = if (withTracking) TrackingData.createMock() else null
            )
        }

        fun fromMLDetection(
            typeString: String,
            confidence: Float,
            boundingBox: BoundingBox,
            position: Position3D? = null
        ): DetectedObject {
            return DetectedObject(
                type = ObjectType.fromString(typeString),
                measurements = ObjectMeasurements.empty(),
                confidence = confidence.coerceIn(0f, 1f),
                position = position,
                boundingBox = boundingBox
            )
        }

        fun createWithMeasurements(
            type: ObjectType,
            measurements: ObjectMeasurements,
            confidence: Float,
            position: Position3D? = null,
            boundingBox: BoundingBox? = null
        ): DetectedObject {
            return DetectedObject(
                type = type,
                measurements = measurements,
                confidence = confidence.coerceIn(0f, 1f),
                position = position,
                boundingBox = boundingBox
            )
        }

        fun createMockList(count: Int = 5): List<DetectedObject> {
            val types = ObjectType.getMeasurableTypes()
            return (1..count).map { index ->
                createMock(
                    type = types.random(),
                    confidence = (0.6f + index * 0.08f).coerceAtMost(0.95f),
                    withPosition = index % 2 == 0,
                    withMeasurements = index % 3 != 0
                )
            }
        }

        fun fromExportData(data: ObjectExportData): DetectedObject? {
            return try {
                DetectedObject(
                    id = data.id,
                    type = ObjectType.fromString(data.type),
                    measurements = ObjectMeasurements.fromExportData(data.measurements),
                    confidence = data.confidence,
                    position = data.position,
                    boundingBox = data.boundingBox?.let { bbox ->
                        BoundingBox(
                            left = bbox["left"]?.toFloat() ?: 0f,
                            top = bbox["top"]?.toFloat() ?: 0f,
                            right = bbox["right"]?.toFloat() ?: 0f,
                            bottom = bbox["bottom"]?.toFloat() ?: 0f
                        )
                    },
                    trackingState = TrackingState.valueOf(data.trackingState),
                    lastUpdated = data.lastUpdated,
                    timestamp = data.timestamp,
                    isStatic = data.isStatic,
                    sessionId = data.sessionId
                )
            } catch (e: Exception) {
                null
            }
        }
    }
}

// ========== ENUMS ==========

@Serializable
enum class ObjectType(
    val displayName: String,
    val category: ObjectCategory,
    val typicalSize: ObjectSize = ObjectSize.MEDIUM,
    val canBeMeasured: Boolean = true,
    val isCommonReference: Boolean = false
) {
    // Pessoas e seres vivos
    PERSON("Pessoa", ObjectCategory.LIVING, ObjectSize.LARGE, true),

    // Recipientes (ideais para medição)
    BOTTLE("Garrafa", ObjectCategory.CONTAINER, ObjectSize.SMALL, true),
    CUP("Copo", ObjectCategory.CONTAINER, ObjectSize.SMALL, true),
    BOX("Caixa", ObjectCategory.CONTAINER, ObjectSize.MEDIUM, true),

    // Móveis
    CHAIR("Cadeira", ObjectCategory.FURNITURE, ObjectSize.LARGE, true),
    TABLE("Mesa", ObjectCategory.FURNITURE, ObjectSize.LARGE, true),
    DESK("Escrivaninha", ObjectCategory.FURNITURE, ObjectSize.LARGE, true),
    SOFA("Sofá", ObjectCategory.FURNITURE, ObjectSize.EXTRA_LARGE, true),

    // Eletrônicos (boas referências)
    PHONE("Celular", ObjectCategory.ELECTRONIC, ObjectSize.SMALL, true, true),
    LAPTOP("Laptop", ObjectCategory.ELECTRONIC, ObjectSize.MEDIUM, true),
    TABLET("Tablet", ObjectCategory.ELECTRONIC, ObjectSize.SMALL, true, true),
    TV("Televisão", ObjectCategory.ELECTRONIC, ObjectSize.LARGE, true),

    // Objetos de referência
    BOOK("Livro", ObjectCategory.ITEM, ObjectSize.SMALL, true, true),
    CREDITCARD("Cartão", ObjectCategory.ITEM, ObjectSize.TINY, true, true),
    COIN("Moeda", ObjectCategory.ITEM, ObjectSize.TINY, true, true),
    RULER("Régua", ObjectCategory.TOOL, ObjectSize.SMALL, true, true),

    // Outros objetos
    BAG("Bolsa", ObjectCategory.ITEM, ObjectSize.MEDIUM, true),
    SHOE("Sapato", ObjectCategory.CLOTHING, ObjectSize.SMALL, true),

    // Genérico
    OBJECT("Objeto", ObjectCategory.ITEM, ObjectSize.MEDIUM, true),
    UNKNOWN("Desconhecido", ObjectCategory.OTHER, ObjectSize.MEDIUM, false);

    companion object {
        fun fromString(value: String): ObjectType {
            return values().find {
                it.name.equals(value, ignoreCase = true) ||
                        it.displayName.equals(value, ignoreCase = true)
            } ?: UNKNOWN
        }

        fun getMeasurableTypes(): List<ObjectType> = values().filter { it.canBeMeasured }

        fun getCalibrationTypes(): List<ObjectType> = values().filter { it.isCommonReference }

        fun getByCategory(category: ObjectCategory): List<ObjectType> {
            return values().filter { it.category == category }
        }

        fun getBySize(size: ObjectSize): List<ObjectType> {
            return values().filter { it.typicalSize == size }
        }
    }
}

@Serializable
enum class ObjectCategory(val displayName: String) {
    LIVING("Seres Vivos"),
    FURNITURE("Móveis"),
    ELECTRONIC("Eletrônicos"),
    CONTAINER("Recipientes"),
    ITEM("Objetos"),
    CLOTHING("Roupas e Acessórios"),
    TOOL("Ferramentas"),
    OTHER("Outros")
}

@Serializable
enum class ObjectSize(val displayName: String, val rangeDescription: String) {
    TINY("Muito Pequeno", "< 5cm"),
    SMALL("Pequeno", "5cm - 30cm"),
    MEDIUM("Médio", "30cm - 100cm"),
    LARGE("Grande", "100cm - 200cm"),
    EXTRA_LARGE("Muito Grande", "> 200cm")
}

@Serializable
enum class TrackingState(val displayName: String) {
    TRACKING("Rastreando"),
    PAUSED("Pausado"),
    LIMITED("Limitado"),
    LOST("Perdido"),
    NOT_TRACKING("Não Rastreando")
}

@Serializable
enum class ConfidenceLevel(val displayName: String, val color: String) {
    VERY_HIGH("Muito Alta", "#4CAF50"),
    HIGH("Alta", "#8BC34A"),
    MEDIUM("Média", "#FF9800"),
    LOW("Baixa", "#FF5722"),
    VERY_LOW("Muito Baixa", "#F44336")
}

@Serializable
enum class QualityLevel(val displayName: String, val description: String) {
    EXCELLENT("Excelente", "Detecção de alta qualidade"),
    GOOD("Boa", "Detecção confiável"),
    FAIR("Razoável", "Detecção aceitável"),
    POOR("Ruim", "Detecção de baixa qualidade"),
    VERY_POOR("Muito Ruim", "Detecção não confiável")
}

// ========== DATA CLASSES ==========

@Serializable
data class Position3D(
    val x: Float,
    val y: Float,
    val z: Float
) {
    fun distanceTo(other: Position3D): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return sqrt(dx*dx + dy*dy + dz*dz)
    }

    fun isValid(): Boolean {
        return !x.isNaN() && !y.isNaN() && !z.isNaN() &&
                x.isFinite() && y.isFinite() && z.isFinite()
    }

    fun normalize(): Position3D {
        val length = sqrt(x*x + y*y + z*z)
        return if (length > 0) {
            Position3D(x/length, y/length, z/length)
        } else {
            Position3D(0f, 0f, 0f)
        }
    }

    companion object {
        fun createMock(): Position3D {
            return Position3D(
                x = (-2f..2f).random(),
                y = (-1f..1f).random(),
                z = (0.5f..3f).random()
            )
        }
    }
}

@Serializable
data class TrackingData(
    val velocity: Float = 0f,
    val direction: Float = 0f,
    val acceleration: Float = 0f,
    val lastPosition: Position3D? = null,
    val lastUpdateTime: Long = System.currentTimeMillis(),
    val trackingQuality: Float = 1.0f
) {
    fun updatePosition(newPosition: Position3D, timestamp: Long): TrackingData {
        val deltaTime = (timestamp - lastUpdateTime) / 1000f // em segundos
        if (deltaTime <= 0 || lastPosition == null) {
            return copy(lastPosition = newPosition, lastUpdateTime = timestamp)
        }

        val distance = lastPosition.distanceTo(newPosition)
        val newVelocity = distance / deltaTime
        val newAcceleration = (newVelocity - velocity) / deltaTime

        return copy(
            velocity = newVelocity,
            acceleration = newAcceleration,
            lastPosition = newPosition,
            lastUpdateTime = timestamp
        )
    }

    companion object {
        fun createMock(): TrackingData {
            return TrackingData(
                velocity = (0f..2f).random(),
                direction = (0f..2*PI).random().toFloat(),
                acceleration = (-1f..1f).random()
            )
        }
    }
}

@Serializable
data class ObjectSummary(
    val id: String,
    val type: ObjectType,
    val displayName: String,
    val confidence: Float,
    val confidenceLevel: ConfidenceLevel,
    val hasPosition: Boolean,
    val hasMeasurements: Boolean,
    val age: String,
    val trackingState: TrackingState
)

@Serializable
data class ObjectExportData(
    val id: String,
    val type: String,
    val displayName: String,
    val measurements: Map<String, String>,
    val confidence: Float,
    val confidenceLevel: String,
    val position: Position3D?,
    val boundingBox: Map<String, String>?,
    val timestamp: Long,
    val lastUpdated: Long,
    val trackingState: String,
    val isStatic: Boolean,
    val sessionId: String?,
    val detailedDescription: String,
    val summary: ObjectSummary
)

@Serializable
data class ObjectMinimalData(
    val id: String,
    val type: ObjectType,
    val confidence: Float,
    val position: Position3D?,
    val timestamp: Long
)

data class ObjectMatchCriteria(
    val requiredType: ObjectType? = null,
    val minConfidence: Float? = null,
    val maxAge: Long? = null,
    val requiredPosition: Position3D? = null,
    val positionTolerance: Float = 1.0f
)

data class UserMeasurementPreferences(
    val preferredLengthUnit: MeasurementUnit = MeasurementUnit.CENTIMETERS,
    val preferredWeightUnit: MeasurementUnit = MeasurementUnit.KILOGRAMS,
    val preferredVolumeUnit: MeasurementUnit = MeasurementUnit.LITERS,
    val preferredTemperatureUnit: MeasurementUnit = MeasurementUnit.CELSIUS,
    val decimalPlaces: Int = 2,
    val useMetricSystem: Boolean = true,
    val locale: Locale = Locale.getDefault()
)

// ========== MEASUREMENT SYSTEM ==========

@Serializable
enum class MeasurementUnit(
    val displayName: String,
    val symbol: String,
    val type: MeasurementType,
    val conversionFactor: Double = 1.0 // Para unidade base do tipo
) {
    // Length (base: metros)
    MILLIMETERS("Milímetros", "mm", MeasurementType.LENGTH, 0.001),
    CENTIMETERS("Centímetros", "cm", MeasurementType.LENGTH, 0.01),
    METERS("Metros", "m", MeasurementType.LENGTH, 1.0),
    KILOMETERS("Quilômetros", "km", MeasurementType.LENGTH, 1000.0),
    INCHES("Polegadas", "in", MeasurementType.LENGTH, 0.0254),
    FEET("Pés", "ft", MeasurementType.LENGTH, 0.3048),
    YARDS("Jardas", "yd", MeasurementType.LENGTH, 0.9144),

    // Weight (base: quilogramas)
    GRAMS("Gramas", "g", MeasurementType.WEIGHT, 0.001),
    KILOGRAMS("Quilogramas", "kg", MeasurementType.WEIGHT, 1.0),
    POUNDS("Libras", "lb", MeasurementType.WEIGHT, 0.453592),
    OUNCES("Onças", "oz", MeasurementType.WEIGHT, 0.0283495),

    // Volume (base: litros)
    MILLILITERS("Mililitros", "ml", MeasurementType.VOLUME, 0.001),
    LITERS("Litros", "l", MeasurementType.VOLUME, 1.0),
    FLUID_OUNCES("Onças Fluidas", "fl oz", MeasurementType.VOLUME, 0.0295735),
    CUPS("Xícaras", "cup", MeasurementType.VOLUME, 0.236588),
    GALLONS("Galões", "gal", MeasurementType.VOLUME, 3.78541),

    // Temperature (base: celsius)
    CELSIUS("Celsius", "°C", MeasurementType.TEMPERATURE, 1.0),
    FAHRENHEIT("Fahrenheit", "°F", MeasurementType.TEMPERATURE, 1.0), // Conversão especial
    KELVIN("Kelvin", "K", MeasurementType.TEMPERATURE, 1.0), // Conversão especial

    // Area (base: metros quadrados)
    SQUARE_METERS("Metros Quadrados", "m²", MeasurementType.AREA, 1.0),
    SQUARE_CENTIMETERS("Centímetros Quadrados", "cm²", MeasurementType.AREA, 0.0001),
    SQUARE_FEET("Pés Quadrados", "ft²", MeasurementType.AREA, 0.092903);

    fun isMetric(): Boolean {
        return when (this) {
            INCHES, FEET, YARDS, POUNDS, OUNCES, FLUID_OUNCES, CUPS, GALLONS, FAHRENHEIT, SQUARE_FEET -> false
            else -> true
        }
    }
}

@Serializable
enum class MeasurementType(val displayName: String) {
    LENGTH("Comprimento"),
    WIDTH("Largura"),
    HEIGHT("Altura"),
    DEPTH("Profundidade"),
    WEIGHT("Peso"),
    VOLUME("Volume"),
    AREA("Área"),
    TEMPERATURE("Temperatura"),
    DISTANCE("Distância")
}

@Serializable
data class Measurement(
    val value: Double,
    val unit: MeasurementUnit,
    val confidence: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(value >= 0) { "Measurement value cannot be negative: $value" }
        require(confidence in 0f..1f) { "Confidence must be between 0 and 1: $confidence" }
    }

    fun isReliable(threshold: Float = 0.7f): Boolean = confidence >= threshold

    fun formatDisplay(decimals: Int = 2): String {
        return "${value.roundTo(decimals)} ${unit.symbol}"
    }

    fun convertTo(targetUnit: MeasurementUnit): Measurement? {
        if (unit.type != targetUnit.type) return null

        val valueInBaseUnit = when (unit.type) {
            MeasurementType.TEMPERATURE -> convertTemperatureToBase(value, unit)
            else -> value * unit.conversionFactor
        }

        val valueInTargetUnit = when (targetUnit.type) {
            MeasurementType.TEMPERATURE -> convertTemperatureFromBase(valueInBaseUnit, targetUnit)
            else -> valueInBaseUnit / targetUnit.conversionFactor
        }

        return copy(value = valueInTargetUnit, unit = targetUnit)
    }

    private fun convertTemperatureToBase(value: Double, unit: MeasurementUnit): Double {
        return when (unit) {
            MeasurementUnit.CELSIUS -> value
            MeasurementUnit.FAHRENHEIT -> (value - 32) * 5.0 / 9.0
            MeasurementUnit.KELVIN -> value - 273.15
            else -> value
        }
    }

    private fun convertTemperatureFromBase(value: Double, unit: MeasurementUnit): Double {
        return when (unit) {
            MeasurementUnit.CELSIUS -> value
            MeasurementUnit.FAHRENHEIT -> value * 9.0 / 5.0 + 32
            MeasurementUnit.KELVIN -> value + 273.15
            else -> value
        }
    }
}

@Serializable
data class ObjectMeasurements(
    val measurements: Map<MeasurementType, Measurement> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
) {
    fun hasAnyMeasurement(): Boolean = measurements.isNotEmpty()

    fun hasAllMeasurements(): Boolean {
        return measurements.containsKey(MeasurementType.LENGTH) &&
                measurements.containsKey(MeasurementType.WIDTH) &&
                measurements.containsKey(MeasurementType.HEIGHT)
    }

    fun getAllMeasurements(): List<Measurement> = measurements.values.toList()

    fun getMeasurementByType(type: MeasurementType): Measurement? = measurements[type]

    fun getPrimaryMeasurement(): Measurement? {
        return measurements[MeasurementType.HEIGHT]
            ?: measurements[MeasurementType.LENGTH]
            ?: measurements[MeasurementType.WIDTH]
            ?: measurements.values.firstOrNull()
    }

    fun getAverageConfidence(): Float {
        val confidences = measurements.values.map { it.confidence }
        return if (confidences.isNotEmpty()) confidences.average().toFloat() else 0f
    }

    fun isValid(): Boolean = measurements.values.all { it.confidence > 0 && it.value >= 0 }

    fun convertTo(preferences: UserMeasurementPreferences): ObjectMeasurements {
        val convertedMeasurements = measurements.mapValues { (type, measurement) ->
            val targetUnit = when (type) {
                MeasurementType.LENGTH, MeasurementType.WIDTH, MeasurementType.HEIGHT,
                MeasurementType.DEPTH, MeasurementType.DISTANCE -> preferences.preferredLengthUnit
                MeasurementType.WEIGHT -> preferences.preferredWeightUnit
                MeasurementType.VOLUME -> preferences.preferredVolumeUnit
                MeasurementType.TEMPERATURE -> preferences.preferredTemperatureUnit
                else -> measurement.unit
            }
            measurement.convertTo(targetUnit) ?: measurement
        }
        return copy(measurements = convertedMeasurements)
    }

    fun toExportData(): Map<String, String> {
        return measurements.mapKeys { it.key.name }.mapValues { (_, measurement) ->
            measurement.formatDisplay()
        }
    }

    companion object {
        fun empty(): ObjectMeasurements = ObjectMeasurements()

        fun createMockForType(type: ObjectType): ObjectMeasurements {
            val measurements = when (type) {
                ObjectType.PHONE -> mapOf(
                    MeasurementType.WIDTH to Measurement(7.0, MeasurementUnit.CENTIMETERS, 0.9f),
                    MeasurementType.HEIGHT to Measurement(15.0, MeasurementUnit.CENTIMETERS, 0.9f),
                    MeasurementType.DEPTH to Measurement(0.8, MeasurementUnit.CENTIMETERS, 0.8f)
                )
                ObjectType.BOTTLE -> mapOf(
                    MeasurementType.HEIGHT to Measurement(25.0, MeasurementUnit.CENTIMETERS, 0.9f),
                    MeasurementType.WIDTH to Measurement(7.0, MeasurementUnit.CENTIMETERS, 0.8f),
                    MeasurementType.VOLUME to Measurement(500.0, MeasurementUnit.MILLILITERS, 0.7f)
                )
                ObjectType.BOOK -> mapOf(
                    MeasurementType.WIDTH to Measurement(15.0, MeasurementUnit.CENTIMETERS, 0.9f),
                    MeasurementType.HEIGHT to Measurement(23.0, MeasurementUnit.CENTIMETERS, 0.9f),
                    MeasurementType.DEPTH to Measurement(2.0, MeasurementUnit.CENTIMETERS, 0.8f)
                )
                else -> mapOf(
                    MeasurementType.LENGTH to Measurement(10.0, MeasurementUnit.CENTIMETERS, 0.7f)
                )
            }
            return ObjectMeasurements(measurements)
        }

        fun forRectangularObject(
            width: Measurement,
            height: Measurement,
            depth: Measurement
        ): ObjectMeasurements {
            return ObjectMeasurements(mapOf(
                MeasurementType.WIDTH to width,
                MeasurementType.HEIGHT to height,
                MeasurementType.DEPTH to depth
            ))
        }

        fun forContainer(
            width: Measurement,
            height: Measurement,
            depth: Measurement,
            volume: Measurement
        ): ObjectMeasurements {
            return ObjectMeasurements(mapOf(
                MeasurementType.WIDTH to width,
                MeasurementType.HEIGHT to height,
                MeasurementType.DEPTH to depth,
                MeasurementType.VOLUME to volume
            ))
        }

        fun forPerson(
            height: Measurement,
            distance: Measurement
        ): ObjectMeasurements {
            return ObjectMeasurements(mapOf(
                MeasurementType.HEIGHT to height,
                MeasurementType.DISTANCE to distance
            ))
        }

        fun fromExportData(data: Map<String, String>): ObjectMeasurements {
            val measurements = data.mapNotNull { (typeStr, valueStr) ->
                try {
                    val type = MeasurementType.valueOf(typeStr)
                    // Parse "value unit" format
                    val parts = valueStr.split(" ")
                    if (parts.size >= 2) {
                        val value = parts[0].toDouble()
                        val unitSymbol = parts.drop(1).joinToString(" ")
                        val unit = MeasurementUnit.values().find { it.symbol == unitSymbol }
                            ?: MeasurementUnit.CENTIMETERS
                        type to Measurement(value, unit)
                    } else null
                } catch (e: Exception) {
                    null
                }
            }.toMap()

            return ObjectMeasurements(measurements)
        }
    }
}