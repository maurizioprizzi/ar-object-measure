package com.objectmeasure.ar.domain.model

import com.objectmeasure.ar.core.util.*
import com.objectmeasure.ar.domain.repository.BoundingBox
import kotlinx.serialization.Serializable
import kotlin.math.*

/**
 * Representa as medições de um objeto detectado - DIA 3 REVISADO
 * Estrutura completa para medições 3D, cálculos derivados e validações
 */
@Serializable
data class ObjectMeasurements(
    // ========== DIMENSÕES BÁSICAS ==========
    val width: Measurement? = null,
    val height: Measurement? = null,
    val depth: Measurement? = null,

    // ========== MEDIÇÕES DERIVADAS ==========
    val area: Measurement? = null,
    val volume: Measurement? = null,
    val perimeter: Measurement? = null,
    val diagonal: Measurement? = null,

    // ========== PROPRIEDADES FÍSICAS ==========
    val weight: Measurement? = null,
    val density: Measurement? = null,

    // ========== POSIÇÃO/ORIENTAÇÃO ==========
    val distance: Measurement? = null,
    val inclination: Measurement? = null,
    val rotation: Measurement? = null,

    // ========== MEDIÇÕES ESPECÍFICAS ==========
    val diameter: Measurement? = null,    // Para objetos circulares
    val radius: Measurement? = null,      // Para objetos circulares
    val circumference: Measurement? = null, // Para objetos circulares

    // ========== METADADOS ==========
    val timestamp: Long = System.currentTimeMillis(),
    val measurementMethod: MeasurementMethod = MeasurementMethod.AR_DETECTION
) {

    // ========== VALIDAÇÕES BÁSICAS ==========

    /**
     * Verifica se pelo menos uma medição está disponível
     */
    fun hasAnyMeasurement(): Boolean {
        return getAllMeasurements().isNotEmpty()
    }

    /**
     * Verifica se todas as medições básicas estão disponíveis
     */
    fun hasAllMeasurements(): Boolean {
        return width != null && height != null && depth != null && weight != null
    }

    /**
     * Verifica se tem dimensões 3D completas
     */
    fun hasComplete3DDimensions(): Boolean {
        return width != null && height != null && depth != null
    }

    /**
     * Verifica se tem dimensões 2D básicas
     */
    fun hasBasic2DDimensions(): Boolean {
        return width != null && height != null
    }

    /**
     * Obtém todas as medições não-nulas
     */
    fun getAllMeasurements(): List<Measurement> {
        return listOfNotNull(
            width, height, depth, area, volume, perimeter, diagonal,
            weight, density, distance, inclination, rotation,
            diameter, radius, circumference
        )
    }

    /**
     * Obtém medições por categoria
     */
    fun getMeasurementsByCategory(category: UnitCategory): List<Measurement> {
        return getAllMeasurements().filter { it.unit.category == category }
    }

    /**
     * Obtém a medição primária (mais importante para o objeto)
     */
    fun getPrimaryMeasurement(): Measurement? {
        return when {
            height != null -> height
            width != null -> width
            diameter != null -> diameter
            volume != null -> volume
            area != null -> area
            depth != null -> depth
            weight != null -> weight
            distance != null -> distance
            else -> getAllMeasurements().firstOrNull()
        }
    }

    // ========== CÁLCULOS AUTOMÁTICOS ==========

    /**
     * Calcula área automaticamente se width e height estão disponíveis
     */
    fun calculateArea(): Measurement? {
        return if (width != null && height != null &&
            width.unit.category == UnitCategory.LENGTH &&
            height.unit.category == UnitCategory.LENGTH) {

            // Converter para mesma unidade
            val heightConverted = height.convertTo(width.unit)
            val areaValue = width.value * heightConverted.value

            val areaUnit = when (width.unit) {
                MeasurementUnit.MILLIMETERS -> MeasurementUnit.SQUARE_MILLIMETERS
                MeasurementUnit.CENTIMETERS -> MeasurementUnit.SQUARE_CENTIMETERS
                MeasurementUnit.METERS -> MeasurementUnit.SQUARE_METERS
                MeasurementUnit.INCHES -> MeasurementUnit.SQUARE_INCHES
                MeasurementUnit.FEET -> MeasurementUnit.SQUARE_FEET
                else -> MeasurementUnit.SQUARE_CENTIMETERS
            }

            val confidence = minOf(width.confidence, height.confidence)
            Measurement(areaValue, areaUnit, confidence)
        } else null
    }

    /**
     * Calcula volume automaticamente para objetos retangulares
     */
    fun calculateVolume(): Measurement? {
        return if (width != null && height != null && depth != null) {
            // Converter todas para mesma unidade
            val heightConverted = height.convertTo(width.unit)
            val depthConverted = depth.convertTo(width.unit)

            val volumeValue = width.value * heightConverted.value * depthConverted.value

            // Volume em unidade cúbica apropriada ou litros
            val volumeUnit = when (width.unit) {
                MeasurementUnit.CENTIMETERS -> MeasurementUnit.LITERS
                MeasurementUnit.METERS -> MeasurementUnit.CUBIC_METERS
                MeasurementUnit.MILLIMETERS -> MeasurementUnit.MILLILITERS
                else -> MeasurementUnit.LITERS
            }

            // Converter para unidade de volume apropriada
            val finalVolumeValue = when (width.unit) {
                MeasurementUnit.CENTIMETERS -> volumeValue / 1000.0 // cm³ to L
                MeasurementUnit.MILLIMETERS -> volumeValue / 1000000.0 // mm³ to mL
                else -> volumeValue
            }

            val confidence = minOf(width.confidence, height.confidence, depth.confidence)
            Measurement(finalVolumeValue, volumeUnit, confidence)
        } else null
    }

    /**
     * Calcula perímetro para objetos retangulares
     */
    fun calculatePerimeter(): Measurement? {
        return if (width != null && height != null) {
            val heightConverted = height.convertTo(width.unit)
            val perimeterValue = 2 * (width.value + heightConverted.value)
            val confidence = minOf(width.confidence, height.confidence)

            Measurement(perimeterValue, width.unit, confidence)
        } else null
    }

    /**
     * Calcula diagonal para objetos retangulares
     */
    fun calculateDiagonal(): Measurement? {
        return if (width != null && height != null) {
            val heightConverted = height.convertTo(width.unit)
            val diagonalValue = sqrt(width.value.pow(2) + heightConverted.value.pow(2))
            val confidence = minOf(width.confidence, height.confidence)

            Measurement(diagonalValue, width.unit, confidence)
        } else null
    }

    /**
     * Calcula diagonal 3D
     */
    fun calculate3DDiagonal(): Measurement? {
        return if (width != null && height != null && depth != null) {
            val heightConverted = height.convertTo(width.unit)
            val depthConverted = depth.convertTo(width.unit)

            val diagonalValue = sqrt(
                width.value.pow(2) +
                        heightConverted.value.pow(2) +
                        depthConverted.value.pow(2)
            )

            val confidence = minOf(width.confidence, height.confidence, depth.confidence)
            Measurement(diagonalValue, width.unit, confidence)
        } else null
    }

    /**
     * Calcula circumferência a partir do diâmetro
     */
    fun calculateCircumference(): Measurement? {
        return diameter?.let { d ->
            val circumferenceValue = PI * d.value
            Measurement(circumferenceValue, d.unit, d.confidence)
        }
    }

    /**
     * Calcula raio a partir do diâmetro
     */
    fun calculateRadius(): Measurement? {
        return diameter?.let { d ->
            val radiusValue = d.value / 2.0
            Measurement(radiusValue, d.unit, d.confidence)
        }
    }

    /**
     * Calcula densidade se volume e peso estão disponíveis
     */
    fun calculateDensity(): Measurement? {
        return if (volume != null && weight != null) {
            // Converter peso para kg e volume para m³
            val weightInKg = weight.convertTo(MeasurementUnit.KILOGRAMS)
            val volumeInM3 = volume.convertTo(MeasurementUnit.CUBIC_METERS)

            if (volumeInM3.value > 0) {
                val densityValue = weightInKg.value / volumeInM3.value
                val confidence = minOf(weight.confidence, volume.confidence)

                // kg/m³ é a unidade padrão de densidade
                Measurement(densityValue, MeasurementUnit.KILOGRAMS, confidence) // Note: seria melhor ter uma unidade específica
            } else null
        } else null
    }

    // ========== VALIDAÇÕES AVANÇADAS ==========

    /**
     * Valida se as medições fazem sentido juntas
     */
    fun isValid(): Boolean {
        // Verificar se medições são positivas
        if (getAllMeasurements().any { it.value <= 0 }) return false

        // Verificar consistência entre medições
        if (!isVolumeConsistent()) return false
        if (!isAreaConsistent()) return false
        if (!isCircularMeasurementsConsistent()) return false

        // Verificar se pelo menos uma medição tem confidence alta
        if (getAllMeasurements().none { it.isReliable() }) return false

        return true
    }

    /**
     * Verifica consistência do volume
     */
    private fun isVolumeConsistent(): Boolean {
        if (volume == null || !hasComplete3DDimensions()) return true

        val calculatedVolume = calculateVolume() ?: return true
        val volumeConverted = volume.convertTo(calculatedVolume.unit)

        val difference = abs(volumeConverted.value - calculatedVolume.value)
        val tolerance = calculatedVolume.value * 0.2 // 20% tolerance

        return difference <= tolerance
    }

    /**
     * Verifica consistência da área
     */
    private fun isAreaConsistent(): Boolean {
        if (area == null || !hasBasic2DDimensions()) return true

        val calculatedArea = calculateArea() ?: return true
        val areaConverted = area.convertTo(calculatedArea.unit)

        val difference = abs(areaConverted.value - calculatedArea.value)
        val tolerance = calculatedArea.value * 0.15 // 15% tolerance

        return difference <= tolerance
    }

    /**
     * Verifica consistência de medições circulares
     */
    private fun isCircularMeasurementsConsistent(): Boolean {
        if (diameter == null) return true

        // Verificar radius vs diameter
        radius?.let { r ->
            val expectedRadius = diameter.value / 2.0
            val radiusConverted = r.convertTo(diameter.unit)
            val difference = abs(radiusConverted.value - expectedRadius)
            val tolerance = expectedRadius * 0.1 // 10% tolerance

            if (difference > tolerance) return false
        }

        // Verificar circumference vs diameter
        circumference?.let { c ->
            val expectedCircumference = PI * diameter.value
            val circumferenceConverted = c.convertTo(diameter.unit)
            val difference = abs(circumferenceConverted.value - expectedCircumference)
            val tolerance = expectedCircumference * 0.1 // 10% tolerance

            if (difference > tolerance) return false
        }

        return true
    }

    // ========== CONVERSÕES ==========

    /**
     * Converte todas as medições para as unidades preferidas do usuário
     */
    fun convertTo(preferences: UserMeasurementPreferences): ObjectMeasurements {
        return copy(
            width = width?.convertTo(preferences.preferredLengthUnit),
            height = height?.convertTo(preferences.preferredLengthUnit),
            depth = depth?.convertTo(preferences.preferredLengthUnit),
            diameter = diameter?.convertTo(preferences.preferredLengthUnit),
            radius = radius?.convertTo(preferences.preferredLengthUnit),
            circumference = circumference?.convertTo(preferences.preferredLengthUnit),
            perimeter = perimeter?.convertTo(preferences.preferredLengthUnit),
            diagonal = diagonal?.convertTo(preferences.preferredLengthUnit),

            area = area?.let {
                val areaUnit = when (preferences.preferredLengthUnit) {
                    MeasurementUnit.MILLIMETERS -> MeasurementUnit.SQUARE_MILLIMETERS
                    MeasurementUnit.CENTIMETERS -> MeasurementUnit.SQUARE_CENTIMETERS
                    MeasurementUnit.METERS -> MeasurementUnit.SQUARE_METERS
                    else -> MeasurementUnit.SQUARE_CENTIMETERS
                }
                it.convertTo(areaUnit)
            },

            volume = volume?.convertTo(preferences.preferredVolumeUnit),
            weight = weight?.convertTo(preferences.preferredWeightUnit),

            distance = distance?.convertTo(preferences.preferredLengthUnit),
            inclination = inclination?.convertTo(MeasurementUnit.DEGREES),
            rotation = rotation?.convertTo(MeasurementUnit.DEGREES)
        )
    }

    /**
     * Converte para melhores unidades automaticamente
     */
    fun toBestUnits(useMetric: Boolean = true): ObjectMeasurements {
        return copy(
            width = width?.toBestUnit(useMetric),
            height = height?.toBestUnit(useMetric),
            depth = depth?.toBestUnit(useMetric),
            area = area?.toBestUnit(useMetric),
            volume = volume?.toBestUnit(useMetric),
            weight = weight?.toBestUnit(useMetric),
            diameter = diameter?.toBestUnit(useMetric),
            radius = radius?.toBestUnit(useMetric),
            circumference = circumference?.toBestUnit(useMetric),
            perimeter = perimeter?.toBestUnit(useMetric),
            diagonal = diagonal?.toBestUnit(useMetric),
            distance = distance?.toBestUnit(useMetric)
        )
    }

    // ========== COMPARAÇÕES ==========

    /**
     * Compara com outro ObjectMeasurements
     */
    fun similarTo(other: ObjectMeasurements, tolerance: Double = 0.1): Boolean {
        return compareMeasurement(width, other.width, tolerance) &&
                compareMeasurement(height, other.height, tolerance) &&
                compareMeasurement(depth, other.depth, tolerance) &&
                compareMeasurement(weight, other.weight, tolerance)
    }

    /**
     * Calcula score de similaridade
     */
    fun similarityScore(other: ObjectMeasurements): Float {
        val comparisons = listOf(
            width to other.width,
            height to other.height,
            depth to other.depth,
            weight to other.weight,
            volume to other.volume
        )

        val scores = comparisons.mapNotNull { (m1, m2) ->
            if (m1 != null && m2 != null) {
                val m2Converted = m2.convertTo(m1.unit)
                val diff = abs(m1.value - m2Converted.value) / m1.value
                (1.0 - diff.coerceAtMost(1.0)).toFloat()
            } else null
        }

        return scores.averageOrNull() ?: 0f
    }

    /**
     * Compara medição individual
     */
    private fun compareMeasurement(m1: Measurement?, m2: Measurement?, tolerance: Double): Boolean {
        return when {
            m1 == null && m2 == null -> true
            m1 == null || m2 == null -> false
            else -> {
                val converted = m2.convertTo(m1.unit)
                abs(m1.value - converted.value) / m1.value <= tolerance
            }
        }
    }

    // ========== FORMATAÇÃO E EXPORT ==========

    /**
     * Formata medições para display
     */
    fun formatForDisplay(preferences: UserMeasurementPreferences): Map<String, String> {
        val converted = convertTo(preferences)
        val result = mutableMapOf<String, String>()

        converted.width?.let { result["Largura"] = it.formatSmart() }
        converted.height?.let { result["Altura"] = it.formatSmart() }
        converted.depth?.let { result["Profundidade"] = it.formatSmart() }
        converted.diameter?.let { result["Diâmetro"] = it.formatSmart() }
        converted.area?.let { result["Área"] = it.formatSmart() }
        converted.volume?.let { result["Volume"] = it.formatSmart() }
        converted.weight?.let { result["Peso"] = it.formatSmart() }
        converted.distance?.let { result["Distância"] = it.formatSmart() }

        return result
    }

    /**
     * Exporta dados para serialização
     */
    fun toExportData(): Map<String, String> {
        val result = mutableMapOf<String, String>()

        width?.let { result["width"] = "${it.value}|${it.unit.symbol}|${it.confidence}" }
        height?.let { result["height"] = "${it.value}|${it.unit.symbol}|${it.confidence}" }
        depth?.let { result["depth"] = "${it.value}|${it.unit.symbol}|${it.confidence}" }
        diameter?.let { result["diameter"] = "${it.value}|${it.unit.symbol}|${it.confidence}" }
        area?.let { result["area"] = "${it.value}|${it.unit.symbol}|${it.confidence}" }
        volume?.let { result["volume"] = "${it.value}|${it.unit.symbol}|${it.confidence}" }
        weight?.let { result["weight"] = "${it.value}|${it.unit.symbol}|${it.confidence}" }
        distance?.let { result["distance"] = "${it.value}|${it.unit.symbol}|${it.confidence}" }

        return result
    }

    /**
     * Resumo das medições mais importantes
     */
    fun getSummary(): String {
        val measurements = mutableListOf<String>()

        width?.let { measurements.add("L: ${it.formatSmart()}") }
        height?.let { measurements.add("A: ${it.formatSmart()}") }
        depth?.let { measurements.add("P: ${it.formatSmart()}") }
        diameter?.let { measurements.add("Ø: ${it.formatSmart()}") }
        volume?.let { measurements.add("Vol: ${it.formatSmart()}") }
        weight?.let { measurements.add("Peso: ${it.formatSmart()}") }

        return measurements.joinToString(" • ")
    }

    companion object {
        /**
         * Cria ObjectMeasurements vazio
         */
        fun empty() = ObjectMeasurements()

        // ========== FACTORY METHODS ==========

        /**
         * Para objetos retangulares (livros, celulares, mesas)
         */
        fun forRectangularObject(
            width: Measurement,
            height: Measurement,
            depth: Measurement?
        ): ObjectMeasurements {
            val measurements = ObjectMeasurements(
                width = width,
                height = height,
                depth = depth
            )

            return measurements.copy(
                area = measurements.calculateArea(),
                volume = depth?.let { measurements.calculateVolume() },
                perimeter = measurements.calculatePerimeter(),
                diagonal = measurements.calculateDiagonal()
            )
        }

        /**
         * Para pessoas (altura e estimativa de peso)
         */
        fun forPerson(
            height: Measurement,
            distance: Measurement
        ): ObjectMeasurements {
            val estimatedWeight = estimateWeightFromHeight(height)

            return ObjectMeasurements(
                height = height,
                weight = estimatedWeight,
                distance = distance
            )
        }

        /**
         * Para recipientes (volume é medição principal)
         */
        fun forContainer(
            width: Measurement,
            height: Measurement,
            depth: Measurement,
            volume: Measurement?
        ): ObjectMeasurements {
            val measurements = ObjectMeasurements(
                width = width,
                height = height,
                depth = depth,
                volume = volume
            )

            return measurements.copy(
                area = measurements.calculateArea(),
                volume = volume ?: measurements.calculateVolume(),
                perimeter = measurements.calculatePerimeter()
            )
        }

        /**
         * Para objetos circulares
         */
        fun forCircularObject(
            diameter: Measurement,
            height: Measurement? = null
        ): ObjectMeasurements {
            val measurements = ObjectMeasurements(
                diameter = diameter,
                height = height
            )

            return measurements.copy(
                radius = measurements.calculateRadius(),
                circumference = measurements.calculateCircumference(),
                area = height?.let { h ->
                    // Área da superfície circular
                    val radius = diameter.value / 2.0
                    val areaValue = PI * radius * radius
                    val areaUnit = when (diameter.unit) {
                        MeasurementUnit.CENTIMETERS -> MeasurementUnit.SQUARE_CENTIMETERS
                        MeasurementUnit.METERS -> MeasurementUnit.SQUARE_METERS
                        else -> MeasurementUnit.SQUARE_CENTIMETERS
                    }
                    Measurement(areaValue, areaUnit, diameter.confidence)
                },
                volume = height?.let { h ->
                    // Volume do cilindro
                    val radius = diameter.value / 2.0
                    val heightConverted = h.convertTo(diameter.unit)
                    val volumeValue = PI * radius * radius * heightConverted.value
                    val volumeUnit = MeasurementUnit.LITERS
                    val finalVolumeValue = when (diameter.unit) {
                        MeasurementUnit.CENTIMETERS -> volumeValue / 1000.0 // cm³ to L
                        MeasurementUnit.MILLIMETERS -> volumeValue / 1000000.0 // mm³ to mL
                        else -> volumeValue
                    }
                    Measurement(finalVolumeValue, volumeUnit, minOf(diameter.confidence, h.confidence))
                }
            )
        }

        /**
         * Cria medições mock para tipo específico
         */
        fun createMockForType(objectType: ObjectType): ObjectMeasurements {
            return when (objectType) {
                ObjectType.PHONE -> forRectangularObject(
                    width = 7.5.withUnit(MeasurementUnit.CENTIMETERS, 0.9f),
                    height = 15.0.withUnit(MeasurementUnit.CENTIMETERS, 0.9f),
                    depth = 0.8.withUnit(MeasurementUnit.CENTIMETERS, 0.8f)
                )

                ObjectType.BOTTLE -> forContainer(
                    width = 6.5.withUnit(MeasurementUnit.CENTIMETERS, 0.85f),
                    height = 25.0.withUnit(MeasurementUnit.CENTIMETERS, 0.9f),
                    depth = 6.5.withUnit(MeasurementUnit.CENTIMETERS, 0.8f),
                    volume = 500.0.withUnit(MeasurementUnit.MILLILITERS, 0.8f)
                )

                ObjectType.PERSON -> forPerson(
                    height = 175.0.withUnit(MeasurementUnit.CENTIMETERS, 0.8f),
                    distance = 2.0.withUnit(MeasurementUnit.METERS, 0.9f)
                )

                ObjectType.BOOK -> forRectangularObject(
                    width = 15.0.withUnit(MeasurementUnit.CENTIMETERS, 0.9f),
                    height = 22.0.withUnit(MeasurementUnit.CENTIMETERS, 0.9f),
                    depth = 2.0.withUnit(MeasurementUnit.CENTIMETERS, 0.85f)
                )

                ObjectType.CREDITCARD -> forRectangularObject(
                    width = 85.6.withUnit(MeasurementUnit.MILLIMETERS, 0.95f),
                    height = 53.98.withUnit(MeasurementUnit.MILLIMETERS, 0.95f),
                    depth = 0.76.withUnit(MeasurementUnit.MILLIMETERS, 0.9f)
                )

                ObjectType.CUP -> forCircularObject(
                    diameter = 8.0.withUnit(MeasurementUnit.CENTIMETERS, 0.85f),
                    height = 9.0.withUnit(MeasurementUnit.CENTIMETERS, 0.8f)
                )

                else -> empty()
            }
        }

        /**
         * Estima peso baseado na altura (para pessoas)
         */
        private fun estimateWeightFromHeight(height: Measurement): Measurement? {
            return if (height.unit.category == UnitCategory.LENGTH) {
                val heightInCm = height.convertTo(MeasurementUnit.CENTIMETERS).value
                // Fórmula simples: peso = (altura_cm - 100) * 0.9
                val estimatedWeight = (heightInCm - 100) * 0.9

                if (estimatedWeight > 0) {
                    Measurement(
                        value = estimatedWeight,
                        unit = MeasurementUnit.KILOGRAMS,
                        confidence = height.confidence * 0.4f // Baixa confiança para estimativa
                    )
                } else null
            } else null
        }

        /**
         * Cria ObjectMeasurements a partir de BoundingBox
         */
        fun fromBoundingBox(
            boundingBox: BoundingBox,
            pixelsPerMeter: Float = 1000f, // Default calibration
            objectType: ObjectType = ObjectType.UNKNOWN
        ): ObjectMeasurements {
            val widthInMeters = boundingBox.width() / pixelsPerMeter
            val heightInMeters = boundingBox.height() / pixelsPerMeter

            val width = Measurement(widthInMeters.toDouble(), MeasurementUnit.METERS, boundingBox.confidence)
            val height = Measurement(heightInMeters.toDouble(), MeasurementUnit.METERS, boundingBox.confidence)

            return forRectangularObject(width, height, null)
        }
    }
}

/**
 * Métodos de medição
 */
@Serializable
enum class MeasurementMethod(val displayName: String) {
    AR_DETECTION("Detecção AR"),
    MANUAL_INPUT("Entrada Manual"),
    REFERENCE_CALIBRATION("Calibração por Referência"),
    ML_ESTIMATION("Estimativa ML"),
    CALCULATED("Calculado"),
    IMPORTED("Importado")
}

// ========== EXTENSION FUNCTIONS ==========

/**
 * Extension para listas de ObjectMeasurements
 */
fun List<ObjectMeasurements>.averageMeasurements(): ObjectMeasurements? {
    if (isEmpty()) return null

    val allWidths = mapNotNull { it.width }
    val allHeights = mapNotNull { it.height }
    val allDepths = mapNotNull { it.depth }
    val allWeights = mapNotNull { it.weight }

    return ObjectMeasurements(
        width = allWidths.averageMeasurement(),
        height = allHeights.averageMeasurement(),
        depth = allDepths.averageMeasurement(),
        weight = allWeights.averageMeasurement()
    )
}

/**
 * Filtra medições confiáveis
 */
fun List<ObjectMeasurements>.filterReliable(threshold: Float = 0.7f): List<ObjectMeasurements> {
    return filter { measurements ->
        measurements.getAllMeasurements().any { it.isReliable(threshold) }
    }
}

/**
 * Encontra medições com maior confidence média
 */
fun List<ObjectMeasurements>.mostReliable(): ObjectMeasurements? {
    return maxByOrNull { measurements ->
        val allMeasurements = measurements.getAllMeasurements()
        if (allMeasurements.isNotEmpty()) {
            allMeasurements.map { it.confidence }.average()
        } else 0.0
    }
}