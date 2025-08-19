package com.objectmeasure.ar.domain.model

import com.objectmeasure.ar.core.util.*
import com.objectmeasure.ar.domain.repository.BoundingBox
import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Representa um objeto detectado pela câmera AR - DIA 3 REVISADO
 * Entidade principal que une detecção + medições + tracking
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
    val lastUpdated: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val isStatic: Boolean = false,
    val sessionId: String? = null
) {
    init {
        require(confidence.isValidConfidence()) {
            "Confidence must be between 0.0 and 1.0, got: $confidence"
        }
        require(lastUpdated <= System.currentTimeMillis()) {
            "Last updated cannot be in the future"
        }
        require(timestamp <= System.currentTimeMillis()) {
            "Timestamp cannot be in the future"
        }
    }

    // ========== CONFIDENCE VALIDATION ==========

    /**
     * Verifica se a detecção é confiável usando extensão
     */
    fun isReliableDetection(threshold: Float = DEFAULT_CONFIDENCE_THRESHOLD): Boolean {
        return confidence.isHighConfidence(threshold)
    }

    /**
     * Verifica se confidence é muito alta
     */
    fun isHighConfidenceDetection(): Boolean {
        return confidence.isHighConfidence(HIGH_CONFIDENCE_THRESHOLD)
    }

    /**
     * Normaliza confidence para range válido (usando extensão)
     */
    fun withNormalizedConfidence(): DetectedObject {
        return copy(confidence = confidence.normalizeConfidence())
    }

    // ========== MEASUREMENT VALIDATION ==========

    /**
     * Verifica se o objeto tem medições úteis
     */
    fun hasUsefulMeasurements(): Boolean {
        return measurements.hasAnyMeasurement()
    }

    /**
     * Verifica se tem medições completas
     */
    fun hasCompleteMeasurements(): Boolean {
        return measurements.hasAllMeasurements()
    }

    /**
     * Verifica se as medições são confiáveis
     */
    fun hasReliableMeasurements(threshold: Float = 0.7f): Boolean {
        return measurements.getAllMeasurements().all { it.isReliable(threshold) }
    }

    // ========== POSITION AND TRACKING ==========

    /**
     * Verifica se o objeto tem posição espacial
     */
    fun hasPosition(): Boolean {
        return position != null
    }

    /**
     * Verifica se o objeto está sendo rastreado adequadamente
     */
    fun isProperlyTracked(): Boolean {
        return trackingState == TrackingState.TRACKING &&
                (System.currentTimeMillis() - lastUpdated) < TRACKING_TIMEOUT_MS
    }

    /**
     * Verifica se o tracking foi perdido
     */
    fun isTrackingLost(): Boolean {
        return trackingState == TrackingState.LOST ||
                (System.currentTimeMillis() - lastUpdated) > TRACKING_TIMEOUT_MS
    }

    /**
     * Calcula distância até outro objeto
     */
    fun distanceTo(other: DetectedObject): Float? {
        return if (position != null && other.position != null) {
            position.distanceTo(other.position)
        } else null
    }

    /**
     * Calcula distância até um ponto 3D
     */
    fun distanceTo(point: Position3D): Float? {
        return position?.distanceTo(point)
    }

    /**
     * Verifica se está próximo de outro objeto
     */
    fun isNearTo(other: DetectedObject, maxDistance: Float): Boolean {
        val distance = distanceTo(other)
        return distance != null && distance <= maxDistance
    }

    // ========== OBJECT UPDATES ==========

    /**
     * Cria uma cópia atualizada do objeto
     */
    fun updateMeasurements(newMeasurements: ObjectMeasurements): DetectedObject {
        return copy(
            measurements = newMeasurements,
            lastUpdated = System.currentTimeMillis()
        )
    }

    /**
     * Atualiza posição e tracking state
     */
    fun updatePosition(
        newPosition: Position3D,
        newTrackingState: TrackingState = TrackingState.TRACKING
    ): DetectedObject {
        return copy(
            position = newPosition,
            trackingState = newTrackingState,
            lastUpdated = System.currentTimeMillis()
        )
    }

    /**
     * Atualiza bounding box
     */
    fun updateBoundingBox(newBoundingBox: BoundingBox): DetectedObject {
        return copy(
            boundingBox = newBoundingBox,
            lastUpdated = System.currentTimeMillis()
        )
    }

    /**
     * Atualiza confidence (com validação)
     */
    fun updateConfidence(newConfidence: Float): DetectedObject {
        require(newConfidence.isValidConfidence()) {
            "Invalid confidence: $newConfidence"
        }
        return copy(
            confidence = newConfidence,
            lastUpdated = System.currentTimeMillis()
        )
    }

    /**
     * Marca objeto como perdido no tracking
     */
    fun markAsLost(): DetectedObject {
        return copy(
            trackingState = TrackingState.LOST,
            lastUpdated = System.currentTimeMillis()
        )
    }

    // ========== DISPLAY AND FORMATTING ==========

    /**
     * Retorna uma descrição do objeto para display
     */
    fun getDisplayName(): String {
        return type.displayName
    }

    /**
     * Retorna descrição detalhada do objeto
     */
    fun getDetailedDescription(): String {
        val desc = StringBuilder(getDisplayName())

        if (hasUsefulMeasurements()) {
            measurements.getPrimaryMeasurement()?.let { primaryMeasurement ->
                desc.append(" (${primaryMeasurement.formatDisplay()})")
            }
        }

        if (position != null) {
            desc.append(" - ${position.z.roundTo(2)}m de distância")
        }

        desc.append(" - ${(confidence * 100).roundTo(1)}% confiança")

        return desc.toString()
    }

    /**
     * Formata confidence como porcentagem
     */
    fun getConfidencePercentage(): String {
        return "${(confidence * 100).roundTo(1)}%"
    }

    // ========== VALIDATION ==========

    /**
     * Valida se o objeto está em estado válido
     */
    fun isValid(): Boolean {
        return confidence.isValidConfidence() &&
                measurements.isValid() &&
                (boundingBox?.isValid() != false) &&
                (position?.isValid() != false)
    }

    /**
     * Verifica se o objeto é recente
     */
    fun isRecent(maxAgeMs: Long = 30_000): Boolean {
        return (System.currentTimeMillis() - timestamp) <= maxAgeMs
    }

    /**
     * Verifica se foi atualizado recentemente
     */
    fun isRecentlyUpdated(maxAgeMs: Long = 5_000): Boolean {
        return (System.currentTimeMillis() - lastUpdated) <= maxAgeMs
    }

    // ========== COMPARISON AND SIMILARITY ==========

    /**
     * Verifica se é similar a outro objeto
     */
    fun isSimilarTo(
        other: DetectedObject,
        positionTolerance: Float = 0.1f,
        timeTolerance: Long = 1000
    ): Boolean {
        // Verificar tipo
        if (type != other.type) return false

        // Verificar tempo
        if (kotlin.math.abs(timestamp - other.timestamp) > timeTolerance) return false

        // Verificar posição se disponível
        val distance = distanceTo(other)
        if (distance != null && distance > positionTolerance) return false

        return true
    }

    /**
     * Calcula score de similaridade com outro objeto
     */
    fun similarityScore(other: DetectedObject): Float {
        if (type != other.type) return 0f

        var score = 0f
        var factors = 0

        // Confidence similarity
        val confidenceDiff = kotlin.math.abs(confidence - other.confidence)
        score += (1f - confidenceDiff)
        factors++

        // Position similarity
        distanceTo(other)?.let { distance ->
            score += kotlin.math.max(0f, 1f - distance / 2f) // 2m max distance
            factors++
        }

        // Time similarity
        val timeDiff = kotlin.math.abs(timestamp - other.timestamp).toFloat()
        score += kotlin.math.max(0f, 1f - timeDiff / 5000f) // 5s max time
        factors++

        return if (factors > 0) score / factors else 0f
    }

    // ========== CONVERSION AND EXPORT ==========

    /**
     * Converte medições para unidade preferida
     */
    fun convertMeasurements(preferences: UserMeasurementPreferences): DetectedObject {
        return copy(measurements = measurements.convertTo(preferences))
    }

    /**
     * Exporta dados essenciais para JSON
     */
    fun toExportData(): ObjectExportData {
        return ObjectExportData(
            id = id,
            type = type.name,
            displayName = getDisplayName(),
            measurements = measurements.toExportData(),
            confidence = confidence,
            position = position,
            timestamp = timestamp,
            detailedDescription = getDetailedDescription()
        )
    }

    companion object {
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.7f
        const val HIGH_CONFIDENCE_THRESHOLD = 0.85f
        const val TRACKING_TIMEOUT_MS = 2000L

        /**
         * Cria DetectedObject para desenvolvimento/testes
         */
        fun createMock(
            type: ObjectType = ObjectType.UNKNOWN,
            confidence: Float = 0.8f,
            withPosition: Boolean = true,
            withMeasurements: Boolean = true
        ): DetectedObject {
            return DetectedObject(
                type = type,
                measurements = if (withMeasurements) {
                    ObjectMeasurements.createMockForType(type)
                } else {
                    ObjectMeasurements.empty()
                },
                confidence = confidence.normalizeConfidence(),
                position = if (withPosition) {
                    Position3D.createMock()
                } else null,
                boundingBox = BoundingBox.createMock()
            )
        }

        /**
         * Cria DetectedObject a partir de dados de ML
         */
        fun fromMLDetection(
            typeString: String,
            confidence: Float,
            boundingBox: BoundingBox,
            position: Position3D? = null
        ): DetectedObject {
            return DetectedObject(
                type = ObjectType.fromString(typeString),
                measurements = ObjectMeasurements.empty(),
                confidence = confidence.normalizeConfidence(),
                position = position,
                boundingBox = boundingBox
            )
        }

        /**
         * Cria DetectedObject com medições completas
         */
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
                confidence = confidence.normalizeConfidence(),
                position = position,
                boundingBox = boundingBox
            )
        }

        /**
         * Cria lista de objetos mock para testes
         */
        fun createMockList(count: Int = 5): List<DetectedObject> {
            val types = ObjectType.values().filter { it != ObjectType.UNKNOWN }
            return (1..count).map { index ->
                createMock(
                    type = types.random(),
                    confidence = (0.6f + index * 0.08f).coerceAtMost(0.95f)
                )
            }
        }
    }
}

/**
 * Tipos de objetos que podem ser detectados - DIA 3 REVISADO
 */
@Serializable
enum class ObjectType(
    val displayName: String,
    val category: ObjectCategory,
    val typicalSize: ObjectSize = ObjectSize.MEDIUM,
    val canBeMeasured: Boolean = true
) {
    // Pessoas e seres vivos
    PERSON("Pessoa", ObjectCategory.LIVING, ObjectSize.LARGE, true),

    // Recipientes (ideais para medição)
    BOTTLE("Garrafa", ObjectCategory.CONTAINER, ObjectSize.SMALL, true),
    CUP("Copo", ObjectCategory.CONTAINER, ObjectSize.SMALL, true),
    BOX("Caixa", ObjectCategory.CONTAINER, ObjectSize.MEDIUM, true),

    // Móveis (excelentes para medição)
    CHAIR("Cadeira", ObjectCategory.FURNITURE, ObjectSize.LARGE, true),
    TABLE("Mesa", ObjectCategory.FURNITURE, ObjectSize.LARGE, true),
    DESK("Escrivaninha", ObjectCategory.FURNITURE, ObjectSize.LARGE, true),
    SOFA("Sofá", ObjectCategory.FURNITURE, ObjectSize.EXTRA_LARGE, true),

    // Eletrônicos (boas referências de tamanho)
    PHONE("Celular", ObjectCategory.ELECTRONIC, ObjectSize.SMALL, true),
    LAPTOP("Laptop", ObjectCategory.ELECTRONIC, ObjectSize.MEDIUM, true),
    TABLET("Tablet", ObjectCategory.ELECTRONIC, ObjectSize.SMALL, true),
    TV("Televisão", ObjectCategory.ELECTRONIC, ObjectSize.LARGE, true),

    // Objetos de referência (ótimos para calibração)
    BOOK("Livro", ObjectCategory.ITEM, ObjectSize.SMALL, true),
    CREDITCARD("Cartão", ObjectCategory.ITEM, ObjectSize.TINY, true),
    COIN("Moeda", ObjectCategory.ITEM, ObjectSize.TINY, true),
    RULER("Régua", ObjectCategory.TOOL, ObjectSize.SMALL, true),

    // Outros objetos comuns
    BAG("Bolsa", ObjectCategory.ITEM, ObjectSize.MEDIUM, true),
    SHOE("Sapato", ObjectCategory.CLOTHING, ObjectSize.SMALL, true),

    // Tipo desconhecido
    UNKNOWN("Objeto Desconhecido", ObjectCategory.OTHER, ObjectSize.MEDIUM, false);

    companion object {
        /**
         * Converte string para ObjectType, com fallback para UNKNOWN
         */
        fun fromString(value: String): ObjectType {
            return values().find {
                it.name.equals(value, ignoreCase = true) ||
                        it.displayName.equals(value, ignoreCase = true)
            } ?: UNKNOWN
        }

        /**
         * Retorna tipos que suportam medição de altura
         */
        fun getHeightMeasurableTypes(): List<ObjectType> {
            return listOf(PERSON, BOTTLE, CHAIR, TABLE, DESK, LAPTOP, BOOK)
        }

        /**
         * Retorna tipos por categoria
         */
        fun getByCategory(category: ObjectCategory): List<ObjectType> {
            return values().filter { it.category == category }
        }

        /**
         * Retorna tipos que podem ser medidos
         */
        fun getMeasurableTypes(): List<ObjectType> {
            return values().filter { it.canBeMeasured }
        }

        /**
         * Retorna tipos ideais para calibração
         */
        fun getCalibrationTypes(): List<ObjectType> {
            return listOf(CREDITCARD, COIN, PHONE, RULER, BOOK)
        }

        /**
         * Retorna tipos por tamanho
         */
        fun getBySize(size: ObjectSize): List<ObjectType> {
            return values().filter { it.typicalSize == size }
        }
    }
}

/**
 * Categorias de objetos
 */
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

/**
 * Tamanhos típicos de objetos
 */
@Serializable
enum class ObjectSize(val displayName: String, val rangeDescription: String) {
    TINY("Muito Pequeno", "< 5cm"),
    SMALL("Pequeno", "5cm - 30cm"),
    MEDIUM("Médio", "30cm - 100cm"),
    LARGE("Grande", "100cm - 200cm"),
    EXTRA_LARGE("Muito Grande", "> 200cm")
}

/**
 * Estados de tracking de objetos
 */
@Serializable
enum class TrackingState(val displayName: String) {
    TRACKING("Rastreando"),
    PAUSED("Pausado"),
    LIMITED("Limitado"),
    LOST("Perdido"),
    NOT_TRACKING("Não Rastreando")
}

/**
 * Posição 3D no espaço AR
 */
@Serializable
data class Position3D(
    val x: Float,
    val y: Float,
    val z: Float
) {
    /**
     * Calcula distância euclidiana até outro ponto
     */
    fun distanceTo(other: Position3D): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return sqrt(dx*dx + dy*dy + dz*dz)
    }

    /**
     * Verifica se a posição é válida
     */
    fun isValid(): Boolean {
        return !x.isNaN() && !y.isNaN() && !z.isNaN() &&
                x.isFinite() && y.isFinite() && z.isFinite()
    }

    /**
     * Normaliza posição para unidade
     */
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

/**
 * Dados para exportação
 */
@Serializable
data class ObjectExportData(
    val id: String,
    val type: String,
    val displayName: String,
    val measurements: Map<String, String>,
    val confidence: Float,
    val position: Position3D?,
    val timestamp: Long,
    val detailedDescription: String
)

/**
 * Preferências de medição do usuário
 */
data class UserMeasurementPreferences(
    val preferredLengthUnit: MeasurementUnit = MeasurementUnit.CENTIMETERS,
    val preferredWeightUnit: MeasurementUnit = MeasurementUnit.KILOGRAMS,
    val preferredVolumeUnit: MeasurementUnit = MeasurementUnit.LITERS,
    val decimalPlaces: Int = 2,
    val useMetricSystem: Boolean = true
)