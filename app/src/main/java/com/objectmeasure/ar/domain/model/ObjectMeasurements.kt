package com.objectmeasure.ar.domain.model

import com.objectmeasure.ar.core.util.*
import com.objectmeasure.ar.domain.repository.BoundingBox
import kotlinx.serialization.Serializable
import kotlin.math.*

/**
 * Sistema de Medições de Objetos - VERSÃO FINAL CONSOLIDADA
 *
 * Resolve todos os conflitos entre os 3 arquivos anteriores:
 * - Estrutura eficiente com cálculos lazy
 * - API consistente e unificada
 * - Performance otimizada com cache
 * - Validações matemáticas robustas
 * - Factory methods especializados
 * - Sistema de qualidade integrado
 *
 * @version 2.3 FINAL
 */

// ========== MEASUREMENT METHODS ==========

@Serializable
enum class MeasurementMethod(val displayName: String, val accuracy: Float) {
    AR_DETECTION("Detecção AR", 0.8f),
    MANUAL_INPUT("Entrada Manual", 0.95f),
    REFERENCE_CALIBRATION("Calibração", 0.9f),
    ML_ESTIMATION("Estimativa ML", 0.7f),
    CALCULATED("Calculado", 0.85f),
    IMPORTED("Importado", 0.6f),
    SENSOR_FUSION("Fusão de Sensores", 0.9f)
}

@Serializable
enum class ObjectShape(val displayName: String) {
    RECTANGULAR("Retangular"),
    CYLINDRICAL("Cilíndrico"),
    SPHERICAL("Esférico"),
    IRREGULAR("Irregular"),
    FLAT("Plano"),
    UNKNOWN("Desconhecido")
}

// ========== CORE MEASUREMENT DATA CLASS ==========

/**
 * Estrutura otimizada que armazena apenas medições essenciais
 * e calcula derivadas on-demand com cache
 */
@Serializable
data class ObjectMeasurements(
    // ========== DIMENSÕES PRIMÁRIAS (sempre armazenadas) ==========
    private val _measurements: Map<MeasurementType, Measurement> = emptyMap(),

    // ========== METADADOS ==========
    val timestamp: Long = getCurrentTimeMillis(),
    val measurementMethod: MeasurementMethod = MeasurementMethod.AR_DETECTION,
    val objectShape: ObjectShape = ObjectShape.UNKNOWN,
    val calibrationData: CalibrationReference? = null,

    // ========== CACHE INTERNO ==========
    @kotlinx.serialization.Transient
    private val calculationCache: MutableMap<String, Measurement> = mutableMapOf()
) {

    // ========== PROPRIEDADES DIRETAS (sem cálculos) ==========

    val width: Measurement? get() = _measurements[MeasurementType.WIDTH]
    val height: Measurement? get() = _measurements[MeasurementType.HEIGHT]
    val depth: Measurement? get() = _measurements[MeasurementType.DEPTH]
    val weight: Measurement? get() = _measurements[MeasurementType.WEIGHT]
    val distance: Measurement? get() = _measurements[MeasurementType.DISTANCE]
    val diameter: Measurement? get() = _measurements[MeasurementType.DIAMETER]
    val volume: Measurement? get() = _measurements[MeasurementType.VOLUME]
    val temperature: Measurement? get() = _measurements[MeasurementType.TEMPERATURE]

    // ========== PROPRIEDADES CALCULADAS (com cache) ==========

    val area: Measurement? get() = getOrCalculate("area") { calculateArea() }
    val perimeter: Measurement? get() = getOrCalculate("perimeter") { calculatePerimeter() }
    val diagonal: Measurement? get() = getOrCalculate("diagonal") { calculateDiagonal() }
    val diagonal3D: Measurement? get() = getOrCalculate("diagonal3D") { calculate3DDiagonal() }
    val radius: Measurement? get() = getOrCalculate("radius") { calculateRadius() }
    val circumference: Measurement? get() = getOrCalculate("circumference") { calculateCircumference() }
    val surfaceArea: Measurement? get() = getOrCalculate("surfaceArea") { calculateSurfaceArea() }
    val density: Measurement? get() = getOrCalculate("density") { calculateDensity() }

    // ========== CORE API ==========

    fun hasAnyMeasurement(): Boolean = _measurements.isNotEmpty()

    fun hasAllBasicMeasurements(): Boolean {
        return when (objectShape) {
            ObjectShape.RECTANGULAR -> width != null && height != null && depth != null
            ObjectShape.CYLINDRICAL -> diameter != null && height != null
            ObjectShape.SPHERICAL -> diameter != null
            ObjectShape.FLAT -> width != null && height != null
            else -> width != null && height != null
        }
    }

    fun hasComplete3DDimensions(): Boolean = width != null && height != null && depth != null

    fun getAllMeasurements(): List<Measurement> = _measurements.values.toList()

    fun getMeasurementByType(type: MeasurementType): Measurement? = _measurements[type]

    fun getPrimaryMeasurement(): Measurement? {
        return when (objectShape) {
            ObjectShape.CYLINDRICAL, ObjectShape.SPHERICAL -> diameter
            ObjectShape.FLAT -> width ?: height
            else -> height ?: width ?: depth ?: diameter ?: volume ?: weight ?: distance
        }
    }

    fun getAverageConfidence(): Float {
        val confidences = _measurements.values.map { it.confidence }
        return if (confidences.isNotEmpty()) confidences.average().toFloat() else 0f
    }

    // ========== CALCULATION METHODS (cached) ==========

    private fun getOrCalculate(key: String, calculator: () -> Measurement?): Measurement? {
        return calculationCache.getOrPut(key) {
            calculator() ?: return null
        }
    }

    private fun calculateArea(): Measurement? {
        return when (objectShape) {
            ObjectShape.RECTANGULAR, ObjectShape.FLAT -> calculateRectangularArea()
            ObjectShape.CYLINDRICAL -> calculateCylinderBaseArea()
            ObjectShape.SPHERICAL -> calculateSphereArea()
            else -> calculateRectangularArea() // Fallback
        }
    }

    private fun calculateRectangularArea(): Measurement? {
        val w = width ?: return null
        val h = height ?: return null

        if (w.unit.category != UnitCategory.LENGTH || h.unit.category != UnitCategory.LENGTH) return null

        val hConverted = h.convertTo(w.unit)
        val areaValue = w.value * hConverted.value
        val areaUnit = getAreaUnit(w.unit)
        val confidence = minOf(w.confidence, h.confidence) * measurementMethod.accuracy

        return Measurement(areaValue, areaUnit, confidence)
    }

    private fun calculateCylinderBaseArea(): Measurement? {
        val d = diameter ?: return null
        val radius = d.value / 2.0
        val areaValue = PI * radius * radius
        val areaUnit = getAreaUnit(d.unit)

        return Measurement(areaValue, areaUnit, d.confidence * measurementMethod.accuracy)
    }

    private fun calculateSphereArea(): Measurement? {
        val d = diameter ?: return null
        val radius = d.value / 2.0
        val areaValue = 4 * PI * radius * radius
        val areaUnit = getAreaUnit(d.unit)

        return Measurement(areaValue, areaUnit, d.confidence * measurementMethod.accuracy)
    }

    private fun calculateVolume(): Measurement? {
        return when (objectShape) {
            ObjectShape.RECTANGULAR -> calculateRectangularVolume()
            ObjectShape.CYLINDRICAL -> calculateCylinderVolume()
            ObjectShape.SPHERICAL -> calculateSphereVolume()
            else -> calculateRectangularVolume() // Fallback
        }
    }

    private fun calculateRectangularVolume(): Measurement? {
        val w = width ?: return null
        val h = height ?: return null
        val d = depth ?: return null

        val hConverted = h.convertTo(w.unit)
        val dConverted = d.convertTo(w.unit)

        val volumeValue = w.value * hConverted.value * dConverted.value
        val volumeUnit = getVolumeUnit(w.unit)
        val convertedVolume = convertToVolumeUnit(volumeValue, w.unit, volumeUnit)
        val confidence = minOf(w.confidence, h.confidence, d.confidence) * measurementMethod.accuracy

        return Measurement(convertedVolume, volumeUnit, confidence)
    }

    private fun calculateCylinderVolume(): Measurement? {
        val d = diameter ?: return null
        val h = height ?: return null

        val hConverted = h.convertTo(d.unit)
        val radius = d.value / 2.0
        val volumeValue = PI * radius * radius * hConverted.value
        val volumeUnit = getVolumeUnit(d.unit)
        val convertedVolume = convertToVolumeUnit(volumeValue, d.unit, volumeUnit)
        val confidence = minOf(d.confidence, h.confidence) * measurementMethod.accuracy

        return Measurement(convertedVolume, volumeUnit, confidence)
    }

    private fun calculateSphereVolume(): Measurement? {
        val d = diameter ?: return null
        val radius = d.value / 2.0
        val volumeValue = (4.0 / 3.0) * PI * radius.pow(3)
        val volumeUnit = getVolumeUnit(d.unit)
        val convertedVolume = convertToVolumeUnit(volumeValue, d.unit, volumeUnit)

        return Measurement(convertedVolume, volumeUnit, d.confidence * measurementMethod.accuracy)
    }

    private fun calculatePerimeter(): Measurement? {
        return when (objectShape) {
            ObjectShape.RECTANGULAR, ObjectShape.FLAT -> {
                val w = width ?: return null
                val h = height ?: return null
                val hConverted = h.convertTo(w.unit)
                val perimeterValue = 2 * (w.value + hConverted.value)
                val confidence = minOf(w.confidence, h.confidence) * measurementMethod.accuracy
                Measurement(perimeterValue, w.unit, confidence)
            }
            ObjectShape.CYLINDRICAL, ObjectShape.SPHERICAL -> circumference
            else -> null
        }
    }

    private fun calculateDiagonal(): Measurement? {
        val w = width ?: return null
        val h = height ?: return null

        val hConverted = h.convertTo(w.unit)
        val diagonalValue = sqrt(w.value.pow(2) + hConverted.value.pow(2))
        val confidence = minOf(w.confidence, h.confidence) * measurementMethod.accuracy

        return Measurement(diagonalValue, w.unit, confidence)
    }

    private fun calculate3DDiagonal(): Measurement? {
        val w = width ?: return null
        val h = height ?: return null
        val d = depth ?: return null

        val hConverted = h.convertTo(w.unit)
        val dConverted = d.convertTo(w.unit)

        val diagonalValue = sqrt(w.value.pow(2) + hConverted.value.pow(2) + dConverted.value.pow(2))
        val confidence = minOf(w.confidence, h.confidence, d.confidence) * measurementMethod.accuracy

        return Measurement(diagonalValue, w.unit, confidence)
    }

    private fun calculateRadius(): Measurement? {
        return diameter?.let { d ->
            val radiusValue = d.value / 2.0
            Measurement(radiusValue, d.unit, d.confidence)
        }
    }

    private fun calculateCircumference(): Measurement? {
        return diameter?.let { d ->
            val circumferenceValue = PI * d.value
            Measurement(circumferenceValue, d.unit, d.confidence)
        }
    }

    private fun calculateSurfaceArea(): Measurement? {
        return when (objectShape) {
            ObjectShape.RECTANGULAR -> {
                val w = width ?: return null
                val h = height ?: return null
                val d = depth ?: return null

                val hConverted = h.convertTo(w.unit)
                val dConverted = d.convertTo(w.unit)

                // 2 * (wh + wd + hd)
                val surfaceValue = 2 * (w.value * hConverted.value +
                        w.value * dConverted.value +
                        hConverted.value * dConverted.value)
                val areaUnit = getAreaUnit(w.unit)
                val confidence = minOf(w.confidence, h.confidence, d.confidence) * measurementMethod.accuracy

                Measurement(surfaceValue, areaUnit, confidence)
            }
            ObjectShape.CYLINDRICAL -> {
                val d = diameter ?: return null
                val h = height ?: return null

                val hConverted = h.convertTo(d.unit)
                val radius = d.value / 2.0

                // 2πr² + 2πrh = 2πr(r + h)
                val surfaceValue = 2 * PI * radius * (radius + hConverted.value)
                val areaUnit = getAreaUnit(d.unit)
                val confidence = minOf(d.confidence, h.confidence) * measurementMethod.accuracy

                Measurement(surfaceValue, areaUnit, confidence)
            }
            ObjectShape.SPHERICAL -> calculateSphereArea()
            else -> null
        }
    }

    private fun calculateDensity(): Measurement? {
        val v = volume ?: calculateVolume() ?: return null
        val w = weight ?: return null

        if (v.value <= 0) return null

        // Converter para unidades padrão: kg/m³
        val weightInKg = w.convertTo(MeasurementUnit.KILOGRAMS)
        val volumeInM3 = v.convertTo(MeasurementUnit.CUBIC_METERS)

        val densityValue = weightInKg.value / volumeInM3.value
        val confidence = minOf(v.confidence, w.confidence) * measurementMethod.accuracy

        // Criar unidade de densidade (kg/m³) - idealmente seria uma unidade específica
        return Measurement(densityValue, MeasurementUnit.KILOGRAMS, confidence)
    }

    // ========== HELPER METHODS ==========

    private fun getAreaUnit(lengthUnit: MeasurementUnit): MeasurementUnit {
        return when (lengthUnit) {
            MeasurementUnit.MILLIMETERS -> MeasurementUnit.SQUARE_MILLIMETERS
            MeasurementUnit.CENTIMETERS -> MeasurementUnit.SQUARE_CENTIMETERS
            MeasurementUnit.METERS -> MeasurementUnit.SQUARE_METERS
            MeasurementUnit.INCHES -> MeasurementUnit.SQUARE_INCHES
            MeasurementUnit.FEET -> MeasurementUnit.SQUARE_FEET
            else -> MeasurementUnit.SQUARE_CENTIMETERS
        }
    }

    private fun getVolumeUnit(lengthUnit: MeasurementUnit): MeasurementUnit {
        return when (lengthUnit) {
            MeasurementUnit.MILLIMETERS -> MeasurementUnit.MILLILITERS
            MeasurementUnit.CENTIMETERS -> MeasurementUnit.LITERS
            MeasurementUnit.METERS -> MeasurementUnit.CUBIC_METERS
            MeasurementUnit.INCHES, MeasurementUnit.FEET -> MeasurementUnit.FLUID_OUNCES
            else -> MeasurementUnit.LITERS
        }
    }

    private fun convertToVolumeUnit(volumeValue: Double, fromUnit: MeasurementUnit, toUnit: MeasurementUnit): Double {
        return when (fromUnit) {
            MeasurementUnit.CENTIMETERS -> when (toUnit) {
                MeasurementUnit.LITERS -> volumeValue / 1000.0 // cm³ to L
                MeasurementUnit.MILLILITERS -> volumeValue // cm³ = mL
                else -> volumeValue
            }
            MeasurementUnit.MILLIMETERS -> when (toUnit) {
                MeasurementUnit.MILLILITERS -> volumeValue / 1000.0 // mm³ to mL
                else -> volumeValue
            }
            MeasurementUnit.METERS -> volumeValue // m³ stays m³
            else -> volumeValue
        }
    }

    // ========== VALIDATION ==========

    fun isValid(): Boolean {
        val measurements = _measurements.values
        if (measurements.isEmpty()) return false

        // Verificar se todas as medições são positivas e finitas
        if (measurements.any { !it.isReasonableValue() }) return false

        // Verificar consistência física básica
        if (!hasPhysicalConsistency()) return false

        // Verificar se pelo menos uma medição tem confidence razoável
        if (measurements.none { it.confidence >= 0.3f }) return false

        return true
    }

    private fun hasPhysicalConsistency(): Boolean {
        // Verificar proporcionalidade básica
        if (width != null && height != null) {
            val w = width!!.convertTo(MeasurementUnit.METERS).value
            val h = height!!.convertTo(MeasurementUnit.METERS).value

            // Objetos não devem ter proporções muito extremas
            val ratio = maxOf(w, h) / minOf(w, h)
            if (ratio > 1000) return false // Máximo 1000:1
        }

        // Verificar densidade se disponível
        density?.let { d ->
            // Densidade deve estar em range razoável (0.1 a 30 g/cm³)
            val densityGPerCm3 = d.value / 1000.0 // Assumindo kg/m³ para g/cm³
            if (densityGPerCm3 < 0.1 || densityGPerCm3 > 30.0) return false
        }

        return true
    }

    // ========== CONVERSIONS ==========

    fun convertTo(preferences: UserMeasurementPreferences): ObjectMeasurements {
        val convertedMeasurements = _measurements.mapValues { (type, measurement) ->
            val targetUnit = when (type) {
                MeasurementType.LENGTH, MeasurementType.WIDTH, MeasurementType.HEIGHT,
                MeasurementType.DEPTH, MeasurementType.DISTANCE, MeasurementType.DIAMETER ->
                    preferences.preferredLengthUnit
                MeasurementType.WEIGHT -> preferences.preferredWeightUnit
                MeasurementType.VOLUME -> preferences.preferredVolumeUnit
                MeasurementType.TEMPERATURE -> preferences.preferredTemperatureUnit
                else -> measurement.unit
            }
            try {
                measurement.convertTo(targetUnit)
            } catch (e: Exception) {
                measurement // Keep original if conversion fails
            }
        }

        return copy(_measurements = convertedMeasurements).also {
            it.calculationCache.clear() // Clear cache after conversion
        }
    }

    fun toBestUnits(useMetric: Boolean = true, objectContext: String? = null): ObjectMeasurements {
        val convertedMeasurements = _measurements.mapValues { (_, measurement) ->
            measurement.toBestUnit(useMetric, objectContext)
        }

        return copy(_measurements = convertedMeasurements).also {
            it.calculationCache.clear()
        }
    }

    // ========== COMPARISON ==========

    fun similarTo(other: ObjectMeasurements, tolerance: Double = 0.15): Boolean {
        val thisTypes = _measurements.keys
        val otherTypes = other._measurements.keys
        val commonTypes = thisTypes.intersect(otherTypes)

        if (commonTypes.isEmpty()) return false

        return commonTypes.all { type ->
            val m1 = _measurements[type]!!
            val m2 = other._measurements[type]!!

            try {
                val m2Converted = m2.convertTo(m1.unit)
                val difference = abs(m1.value - m2Converted.value) / m1.value
                difference <= tolerance
            } catch (e: Exception) {
                false
            }
        }
    }

    fun similarityScore(other: ObjectMeasurements): Float {
        val thisTypes = _measurements.keys
        val otherTypes = other._measurements.keys
        val commonTypes = thisTypes.intersect(otherTypes)

        if (commonTypes.isEmpty()) return 0f

        val scores = commonTypes.mapNotNull { type ->
            val m1 = _measurements[type]!!
            val m2 = other._measurements[type]!!

            try {
                val m2Converted = m2.convertTo(m1.unit)
                val difference = abs(m1.value - m2Converted.value) / m1.value
                (1.0 - difference.coerceAtMost(1.0)).toFloat()
            } catch (e: Exception) {
                null
            }
        }

        return scores.averageOrNull() ?: 0f
    }

    // ========== EXPORT/DISPLAY ==========

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

    fun toExportData(): Map<String, String> {
        return _measurements.mapKeys { it.key.name }.mapValues { (_, measurement) ->
            "${measurement.value}|${measurement.unit.symbol}|${measurement.confidence}"
        }
    }

    fun getSummary(): String {
        val primary = getPrimaryMeasurement()
        val secondary = _measurements.values.filter { it != primary }.take(2)

        val parts = mutableListOf<String>()
        primary?.let { parts.add(it.formatSmart()) }
        secondary.forEach { parts.add(it.formatCompact()) }

        return parts.joinToString(" • ")
    }

    // ========== FACTORY METHODS ==========

    companion object {
        private fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

        fun empty(): ObjectMeasurements = ObjectMeasurements()

        fun fromMap(measurements: Map<MeasurementType, Measurement>): ObjectMeasurements {
            return ObjectMeasurements(_measurements = measurements)
        }

        fun builder(): MeasurementBuilder = MeasurementBuilder()

        // ========== SPECIALIZED FACTORIES ==========

        fun forRectangularObject(
            width: Measurement,
            height: Measurement,
            depth: Measurement? = null,
            weight: Measurement? = null
        ): ObjectMeasurements {
            val measurements = mutableMapOf<MeasurementType, Measurement>()
            measurements[MeasurementType.WIDTH] = width
            measurements[MeasurementType.HEIGHT] = height
            depth?.let { measurements[MeasurementType.DEPTH] = it }
            weight?.let { measurements[MeasurementType.WEIGHT] = it }

            return ObjectMeasurements(
                _measurements = measurements,
                objectShape = ObjectShape.RECTANGULAR,
                measurementMethod = MeasurementMethod.AR_DETECTION
            )
        }

        fun forCylindricalObject(
            diameter: Measurement,
            height: Measurement,
            weight: Measurement? = null
        ): ObjectMeasurements {
            val measurements = mutableMapOf<MeasurementType, Measurement>()
            measurements[MeasurementType.DIAMETER] = diameter
            measurements[MeasurementType.HEIGHT] = height
            weight?.let { measurements[MeasurementType.WEIGHT] = it }

            return ObjectMeasurements(
                _measurements = measurements,
                objectShape = ObjectShape.CYLINDRICAL,
                measurementMethod = MeasurementMethod.AR_DETECTION
            )
        }

        fun forSphericalObject(
            diameter: Measurement,
            weight: Measurement? = null
        ): ObjectMeasurements {
            val measurements = mutableMapOf<MeasurementType, Measurement>()
            measurements[MeasurementType.DIAMETER] = diameter
            weight?.let { measurements[MeasurementType.WEIGHT] = it }

            return ObjectMeasurements(
                _measurements = measurements,
                objectShape = ObjectShape.SPHERICAL,
                measurementMethod = MeasurementMethod.AR_DETECTION
            )
        }

        fun forPerson(
            height: Measurement,
            distance: Measurement? = null
        ): ObjectMeasurements {
            val measurements = mutableMapOf<MeasurementType, Measurement>()
            measurements[MeasurementType.HEIGHT] = height
            distance?.let { measurements[MeasurementType.DISTANCE] = it }

            // Não estimar peso - deixar para entrada manual se necessário
            return ObjectMeasurements(
                _measurements = measurements,
                objectShape = ObjectShape.IRREGULAR,
                measurementMethod = MeasurementMethod.AR_DETECTION
            )
        }

        fun createMockForType(objectType: ObjectType): ObjectMeasurements {
            return when (objectType) {
                ObjectType.PHONE -> forRectangularObject(
                    width = 7.5.withUnit(MeasurementUnit.CENTIMETERS, 0.9f),
                    height = 15.0.withUnit(MeasurementUnit.CENTIMETERS, 0.9f),
                    depth = 0.8.withUnit(MeasurementUnit.CENTIMETERS, 0.8f),
                    weight = 180.0.withUnit(MeasurementUnit.GRAMS, 0.7f)
                )

                ObjectType.BOTTLE -> forCylindricalObject(
                    diameter = 6.5.withUnit(MeasurementUnit.CENTIMETERS, 0.85f),
                    height = 25.0.withUnit(MeasurementUnit.CENTIMETERS, 0.9f),
                    weight = 500.0.withUnit(MeasurementUnit.GRAMS, 0.8f)
                )

                ObjectType.PERSON -> forPerson(
                    height = 175.0.withUnit(MeasurementUnit.CENTIMETERS, 0.8f),
                    distance = 2.0.withUnit(MeasurementUnit.METERS, 0.9f)
                )

                ObjectType.BOOK -> forRectangularObject(
                    width = 15.0.withUnit(MeasurementUnit.CENTIMETERS, 0.9f),
                    height = 22.0.withUnit(MeasurementUnit.CENTIMETERS, 0.9f),
                    depth = 2.0.withUnit(MeasurementUnit.CENTIMETERS, 0.85f),
                    weight = 300.0.withUnit(MeasurementUnit.GRAMS, 0.8f)
                )

                ObjectType.CUP -> forCylindricalObject(
                    diameter = 8.0.withUnit(MeasurementUnit.CENTIMETERS, 0.85f),
                    height = 9.0.withUnit(MeasurementUnit.CENTIMETERS, 0.8f)
                )

                else -> empty()
            }
        }

        fun fromBoundingBox(
            boundingBox: BoundingBox,
            calibrationData: CalibrationData? = null,
            objectType: ObjectType = ObjectType.OBJECT
        ): ObjectMeasurements {
            val pixelsPerMeter = calibrationData?.pixelsPerMeter ?: 1000f

            val widthInMeters = boundingBox.width() / pixelsPerMeter
            val heightInMeters = boundingBox.height() / pixelsPerMeter

            val confidence = boundingBox.confidence.coerceIn(0f, 1f)

            return forRectangularObject(
                width = widthInMeters.toDouble().withUnit(MeasurementUnit.METERS, confidence),
                height = heightInMeters.toDouble().withUnit(MeasurementUnit.METERS, confidence)
            )
        }

        fun fromExportData(data: Map<String, String>): ObjectMeasurements {
            val measurements = data.mapNotNull { (typeStr, valueStr) ->
                try {
                    val type = MeasurementType.valueOf(typeStr)
                    val parts = valueStr.split("|")
                    if (parts.size >= 3) {
                        val value = parts[0].toDouble()
                        val unit = MeasurementUnit.fromSymbol(parts[1])
                        val confidence = parts[2].toFloat()

                        if (unit != null) {
                            type to Measurement(value, unit, confidence)
                        } else null
                    } else null
                } catch (e: Exception) {
                    null
                }
            }.toMap()

            return ObjectMeasurements(_measurements = measurements)
        }
    }
}

// ========== BUILDER PATTERN ==========

class MeasurementBuilder {
    private val measurements = mutableMapOf<MeasurementType, Measurement>()
    private var objectShape = ObjectShape.UNKNOWN
    private var measurementMethod = MeasurementMethod.AR_DETECTION
    private var calibrationData: CalibrationReference? = null

    fun width(measurement: Measurement) = apply { measurements[MeasurementType.WIDTH] = measurement }
    fun height(measurement: Measurement) = apply { measurements[MeasurementType.HEIGHT] = measurement }
    fun depth(measurement: Measurement) = apply { measurements[MeasurementType.DEPTH] = measurement }
    fun weight(measurement: Measurement) = apply { measurements[MeasurementType.WEIGHT] = measurement }
    fun diameter(measurement: Measurement) = apply { measurements[MeasurementType.DIAMETER] = measurement }
    fun volume(measurement: Measurement) = apply { measurements[MeasurementType.VOLUME] = measurement }
    fun distance(measurement: Measurement) = apply { measurements[MeasurementType.DISTANCE] = measurement }

    fun shape(shape: ObjectShape) = apply { objectShape = shape }
    fun method(method: MeasurementMethod) = apply { measurementMethod = method }
    fun calibration(calibration: CalibrationReference) = apply { calibrationData = calibration }

    fun build(): ObjectMeasurements {
        return ObjectMeasurements(
            _measurements = measurements.toMap(),
            objectShape = objectShape,
            measurementMethod = measurementMethod,
            calibrationData = calibrationData
        )
    }
}

// ========== SUPPORT DATA CLASSES ==========

@Serializable
data class CalibrationReference(
    val referenceObjectType: ObjectType,
    val pixelsPerMeter: Float,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)

// ========== EXTENSION FUNCTIONS ==========

fun List<ObjectMeasurements>.averageMeasurements(): ObjectMeasurements? {
    if (isEmpty()) return null

    // Agrupar medições por tipo
    val measurementsByType = mutableMapOf<MeasurementType, MutableList<Measurement>>()

    forEach { objMeasurement ->
        objMeasurement.getAllMeasurements().forEach { measurement ->
            objMeasurement._measurements.forEach { (type, meas) ->
                if (meas == measurement) {
                    measurementsByType.getOrPut(type) { mutableListOf() }.add(measurement)
                }
            }
        }
    }

    // Calcular médias por tipo
    val averagedMeasurements = measurementsByType.mapValues { (_, measurements) ->
        measurements.averageMeasurement()
    }.filterValues { it != null }.mapValues { it!! }

    return if (averagedMeasurements.isNotEmpty()) {
        ObjectMeasurements.fromMap(averagedMeasurements)
    } else null
}

fun List<ObjectMeasurements>.filterReliable(threshold: Float = 0.7f): List<ObjectMeasurements> {
    return filter { it.getAverageConfidence() >= threshold }
}

fun List<ObjectMeasurements>.mostReliable(): ObjectMeasurements? {
    return maxByOrNull { it.getAverageConfidence() }
}