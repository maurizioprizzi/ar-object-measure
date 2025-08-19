package com.objectmeasure.ar.domain.model

import com.objectmeasure.ar.core.util.*
import kotlinx.serialization.Serializable
import kotlin.math.*

/**
 * Unidades de medida suportadas pela aplicação - DIA 3 REVISADO
 * Sistema completo com conversões automáticas e categorização
 */
@Serializable
enum class MeasurementUnit(
    val symbol: String,
    val displayName: String,
    val category: UnitCategory,
    val baseMultiplier: Double = 1.0, // Multiplicador para unidade base da categoria
    val isMetric: Boolean = true
) {
    // ========== COMPRIMENTO (base: metros) ==========
    MILLIMETERS("mm", "Milímetros", UnitCategory.LENGTH, 0.001, true),
    CENTIMETERS("cm", "Centímetros", UnitCategory.LENGTH, 0.01, true),
    METERS("m", "Metros", UnitCategory.LENGTH, 1.0, true),
    KILOMETERS("km", "Quilômetros", UnitCategory.LENGTH, 1000.0, true),

    // Sistema imperial
    INCHES("in", "Polegadas", UnitCategory.LENGTH, 0.0254, false),
    FEET("ft", "Pés", UnitCategory.LENGTH, 0.3048, false),
    YARDS("yd", "Jardas", UnitCategory.LENGTH, 0.9144, false),

    // ========== ÁREA (base: metros quadrados) ==========
    SQUARE_MILLIMETERS("mm²", "Milímetros²", UnitCategory.AREA, 0.000001, true),
    SQUARE_CENTIMETERS("cm²", "Centímetros²", UnitCategory.AREA, 0.0001, true),
    SQUARE_METERS("m²", "Metros²", UnitCategory.AREA, 1.0, true),
    SQUARE_KILOMETERS("km²", "Quilômetros²", UnitCategory.AREA, 1000000.0, true),

    // Sistema imperial - área
    SQUARE_INCHES("in²", "Polegadas²", UnitCategory.AREA, 0.00064516, false),
    SQUARE_FEET("ft²", "Pés²", UnitCategory.AREA, 0.092903, false),

    // ========== VOLUME (base: litros) ==========
    MILLILITERS("ml", "Mililitros", UnitCategory.VOLUME, 0.001, true),
    LITERS("L", "Litros", UnitCategory.VOLUME, 1.0, true),
    CUBIC_CENTIMETERS("cm³", "Centímetros³", UnitCategory.VOLUME, 0.001, true),
    CUBIC_METERS("m³", "Metros³", UnitCategory.VOLUME, 1000.0, true),

    // Sistema imperial - volume
    FLUID_OUNCES("fl oz", "Onças Fluidas", UnitCategory.VOLUME, 0.0295735, false),
    CUPS("cup", "Xícaras", UnitCategory.VOLUME, 0.236588, false),
    GALLONS("gal", "Galões", UnitCategory.VOLUME, 3.78541, false),

    // ========== PESO/MASSA (base: quilogramas) ==========
    MILLIGRAMS("mg", "Miligramas", UnitCategory.WEIGHT, 0.000001, true),
    GRAMS("g", "Gramas", UnitCategory.WEIGHT, 0.001, true),
    KILOGRAMS("kg", "Quilogramas", UnitCategory.WEIGHT, 1.0, true),
    TONNES("t", "Toneladas", UnitCategory.WEIGHT, 1000.0, true),

    // Sistema imperial - peso
    OUNCES("oz", "Onças", UnitCategory.WEIGHT, 0.0283495, false),
    POUNDS("lb", "Libras", UnitCategory.WEIGHT, 0.453592, false),

    // ========== ÂNGULO (base: graus) ==========
    DEGREES("°", "Graus", UnitCategory.ANGLE, 1.0, true),
    RADIANS("rad", "Radianos", UnitCategory.ANGLE, 57.2958, true), // 180/π

    // ========== TEMPERATURA (base: Celsius) ==========
    CELSIUS("°C", "Celsius", UnitCategory.TEMPERATURE, 1.0, true),
    FAHRENHEIT("°F", "Fahrenheit", UnitCategory.TEMPERATURE, 1.0, false), // Conversão especial
    KELVIN("K", "Kelvin", UnitCategory.TEMPERATURE, 1.0, true); // Conversão especial

    // ========== CONVERSÃO DE UNIDADES ==========

    /**
     * Converte valor desta unidade para outra da mesma categoria
     */
    fun convertTo(value: Double, targetUnit: MeasurementUnit): Double {
        require(this.category == targetUnit.category) {
            "Cannot convert between different categories: ${this.category} -> ${targetUnit.category}"
        }

        return when (category) {
            UnitCategory.TEMPERATURE -> convertTemperature(value, targetUnit)
            else -> {
                // Converter para unidade base, depois para unidade alvo
                val baseValue = value * this.baseMultiplier
                baseValue / targetUnit.baseMultiplier
            }
        }
    }

    /**
     * Conversão especial para temperatura
     */
    private fun convertTemperature(value: Double, targetUnit: MeasurementUnit): Double {
        // Converter para Celsius primeiro
        val celsius = when (this) {
            CELSIUS -> value
            FAHRENHEIT -> (value - 32) * 5/9
            KELVIN -> value - 273.15
            else -> throw IllegalArgumentException("Invalid temperature unit: $this")
        }

        // Converter de Celsius para unidade alvo
        return when (targetUnit) {
            CELSIUS -> celsius
            FAHRENHEIT -> celsius * 9/5 + 32
            KELVIN -> celsius + 273.15
            else -> throw IllegalArgumentException("Invalid target temperature unit: $targetUnit")
        }
    }

    // ========== FORMATAÇÃO ==========

    /**
     * Formata valor com a unidade
     */
    fun format(value: Double, precision: Int = 2): String {
        return when {
            precision == 0 -> "${value.roundToInt()} $symbol"
            else -> "%.${precision}f %s".format(value, symbol)
        }
    }

    /**
     * Formata valor de forma compacta
     */
    fun formatCompact(value: Double): String {
        return when {
            value >= 1000000 -> "%.1fM %s".format(value / 1000000, symbol)
            value >= 1000 -> "%.1fk %s".format(value / 1000, symbol)
            value >= 100 -> "%.0f %s".format(value, symbol)
            value >= 10 -> "%.1f %s".format(value, symbol)
            else -> "%.2f %s".format(value, symbol)
        }
    }

    /**
     * Formata valor de forma inteligente baseado no valor
     */
    fun formatSmart(value: Double): String {
        val precision = when {
            value < 0.01 -> 4
            value < 0.1 -> 3
            value < 1.0 -> 2
            value < 10.0 -> 2
            value < 100.0 -> 1
            else -> 0
        }
        return format(value, precision)
    }

    // ========== VALIDAÇÕES ==========

    /**
     * Verifica se valor é válido para esta unidade
     */
    fun isValidValue(value: Double): Boolean {
        return when {
            !value.isFinite() -> false
            category == UnitCategory.LENGTH && value < 0 -> false
            category == UnitCategory.AREA && value < 0 -> false
            category == UnitCategory.VOLUME && value < 0 -> false
            category == UnitCategory.WEIGHT && value < 0 -> false
            category == UnitCategory.TEMPERATURE && this == KELVIN && value < 0 -> false
            else -> true
        }
    }

    companion object {
        // ========== DEFAULTS POR CATEGORIA ==========

        fun getDefaultForHeight() = CENTIMETERS
        fun getDefaultForWeight() = KILOGRAMS
        fun getDefaultForDistance() = METERS
        fun getDefaultForAngle() = DEGREES
        fun getDefaultForArea() = SQUARE_METERS
        fun getDefaultForVolume() = LITERS
        fun getDefaultForTemperature() = CELSIUS

        /**
         * Retorna unidade padrão para uma categoria
         */
        fun getDefaultForCategory(category: UnitCategory): MeasurementUnit {
            return when (category) {
                UnitCategory.LENGTH -> METERS
                UnitCategory.AREA -> SQUARE_METERS
                UnitCategory.VOLUME -> LITERS
                UnitCategory.WEIGHT -> KILOGRAMS
                UnitCategory.ANGLE -> DEGREES
                UnitCategory.TEMPERATURE -> CELSIUS
            }
        }

        // ========== CONSULTAS POR CATEGORIA ==========

        /**
         * Obtém todas as unidades de uma categoria
         */
        fun getUnitsByCategory(category: UnitCategory): List<MeasurementUnit> {
            return values().filter { it.category == category }
        }

        /**
         * Obtém unidades métricas de uma categoria
         */
        fun getMetricUnits(category: UnitCategory): List<MeasurementUnit> {
            return values().filter { it.category == category && it.isMetric }
        }

        /**
         * Obtém unidades imperiais de uma categoria
         */
        fun getImperialUnits(category: UnitCategory): List<MeasurementUnit> {
            return values().filter { it.category == category && !it.isMetric }
        }

        // ========== UNIDADES ESPECÍFICAS PARA OBJETOS ==========

        /**
         * Retorna unidade mais apropriada para um valor e categoria
         */
        fun getBestUnitForValue(value: Double, category: UnitCategory, useMetric: Boolean = true): MeasurementUnit {
            val availableUnits = if (useMetric) getMetricUnits(category) else getImperialUnits(category)
            if (availableUnits.isEmpty()) return getDefaultForCategory(category)

            return when (category) {
                UnitCategory.LENGTH -> when {
                    useMetric -> when {
                        value < 0.01 -> MILLIMETERS
                        value < 1.0 -> CENTIMETERS
                        value < 1000.0 -> METERS
                        else -> KILOMETERS
                    }
                    else -> when {
                        value < 0.0833 -> INCHES  // < 1 foot
                        value < 1760.0 -> FEET    // < 1 mile
                        else -> YARDS
                    }
                }
                UnitCategory.WEIGHT -> when {
                    useMetric -> when {
                        value < 0.001 -> MILLIGRAMS
                        value < 1.0 -> GRAMS
                        value < 1000.0 -> KILOGRAMS
                        else -> TONNES
                    }
                    else -> when {
                        value < 1.0 -> OUNCES
                        else -> POUNDS
                    }
                }
                UnitCategory.VOLUME -> when {
                    useMetric -> when {
                        value < 1.0 -> MILLILITERS
                        else -> LITERS
                    }
                    else -> when {
                        value < 1.0 -> FLUID_OUNCES
                        value < 16.0 -> CUPS
                        else -> GALLONS
                    }
                }
                else -> getDefaultForCategory(category)
            }
        }

        /**
         * Unidade preferida para tipo de objeto específico
         */
        fun getPreferredUnitForObjectType(objectType: ObjectType): MeasurementUnit {
            return when (objectType) {
                ObjectType.CREDITCARD, ObjectType.COIN -> MILLIMETERS
                ObjectType.PHONE, ObjectType.BOOK, ObjectType.BOTTLE -> CENTIMETERS
                ObjectType.PERSON, ObjectType.TABLE, ObjectType.CHAIR -> CENTIMETERS
                ObjectType.SOFA, ObjectType.DESK -> METERS
                else -> CENTIMETERS
            }
        }

        /**
         * Lista de unidades para calibração (tamanhos conhecidos)
         */
        fun getCalibrationUnits(): List<MeasurementUnit> {
            return listOf(MILLIMETERS, CENTIMETERS, INCHES)
        }

        // ========== VALIDAÇÕES ==========

        /**
         * Verifica se conversão é possível
         */
        fun canConvert(from: MeasurementUnit, to: MeasurementUnit): Boolean {
            return from.category == to.category
        }

        /**
         * Busca unidade por símbolo
         */
        fun fromSymbol(symbol: String): MeasurementUnit? {
            return values().find { it.symbol.equals(symbol, ignoreCase = true) }
        }

        /**
         * Busca unidade por nome
         */
        fun fromDisplayName(displayName: String): MeasurementUnit? {
            return values().find { it.displayName.equals(displayName, ignoreCase = true) }
        }

        /**
         * Busca unidade por símbolo ou nome
         */
        fun fromString(text: String): MeasurementUnit? {
            return fromSymbol(text) ?: fromDisplayName(text)
        }
    }
}

/**
 * Categorias de unidades de medida
 */
@Serializable
enum class UnitCategory(val displayName: String) {
    LENGTH("Comprimento"),
    AREA("Área"),
    VOLUME("Volume"),
    WEIGHT("Peso"),
    ANGLE("Ângulo"),
    TEMPERATURE("Temperatura")
}

/**
 * Representa uma medição individual com valor, unidade e confiança - REVISADO
 */
@Serializable
data class Measurement(
    val value: Double,
    val unit: MeasurementUnit,
    val confidence: Float = 1.0f,
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(unit.isValidValue(value)) { "Invalid value $value for unit $unit" }
        require(confidence.isValidConfidence()) { "Confidence must be between 0.0 and 1.0" }
    }

    // ========== CONVERSÕES ==========

    /**
     * Converte para outra unidade da mesma categoria
     */
    fun convertTo(targetUnit: MeasurementUnit): Measurement {
        require(unit.category == targetUnit.category) {
            "Cannot convert ${unit.category} to ${targetUnit.category}"
        }

        val convertedValue = unit.convertTo(value, targetUnit)
        return copy(value = convertedValue, unit = targetUnit)
    }

    /**
     * Retorna em unidade mais apropriada para o valor
     */
    fun toBestUnit(useMetric: Boolean = true): Measurement {
        val bestUnit = MeasurementUnit.getBestUnitForValue(value, unit.category, useMetric)
        return if (bestUnit != unit) convertTo(bestUnit) else this
    }

    /**
     * Converte para unidade padrão da categoria
     */
    fun toBaseUnit(): Measurement {
        val baseUnit = MeasurementUnit.getDefaultForCategory(unit.category)
        return convertTo(baseUnit)
    }

    // ========== FORMATAÇÃO ==========

    /**
     * Formata com precisão configurável
     */
    fun formatDisplay(precision: Int = 2): String {
        return unit.format(value, precision)
    }

    /**
     * Formatação compacta
     */
    fun formatCompact(): String {
        return unit.formatCompact(value)
    }

    /**
     * Formatação inteligente
     */
    fun formatSmart(): String {
        return unit.formatSmart(value)
    }

    // ========== VALIDAÇÕES ==========

    /**
     * Verifica confiabilidade com threshold configurável
     */
    fun isReliable(threshold: Float = DEFAULT_CONFIDENCE_THRESHOLD): Boolean {
        return confidence >= threshold
    }

    /**
     * Verifica se é medição recente
     */
    fun isRecent(maxAgeMs: Long = 30_000): Boolean {
        return (System.currentTimeMillis() - timestamp) <= maxAgeMs
    }

    /**
     * Verifica se valor está em range razoável para o tipo
     */
    fun isReasonableValue(): Boolean {
        return when (unit.category) {
            UnitCategory.LENGTH -> value > 0 && value < 1000 // 0 to 1km
            UnitCategory.WEIGHT -> value > 0 && value < 10000 // 0 to 10 tons
            UnitCategory.VOLUME -> value > 0 && value < 100000 // 0 to 100k liters
            UnitCategory.AREA -> value > 0 && value < 1000000 // 0 to 1M m²
            UnitCategory.ANGLE -> value >= 0 && value <= 360 // 0 to 360 degrees
            UnitCategory.TEMPERATURE -> when (unit) {
                MeasurementUnit.CELSIUS -> value > -100 && value < 100
                MeasurementUnit.FAHRENHEIT -> value > -150 && value < 200
                MeasurementUnit.KELVIN -> value >= 0 && value < 400
                else -> true
            }
        }
    }

    // ========== OPERAÇÕES MATEMÁTICAS ==========

    /**
     * Soma com outra medição (converte unidades se necessário)
     */
    operator fun plus(other: Measurement): Measurement {
        require(unit.category == other.unit.category) {
            "Cannot add measurements of different categories"
        }
        val otherConverted = other.convertTo(unit)
        return Measurement(
            value = value + otherConverted.value,
            unit = unit,
            confidence = minOf(confidence, otherConverted.confidence)
        )
    }

    /**
     * Subtração com outra medição
     */
    operator fun minus(other: Measurement): Measurement {
        require(unit.category == other.unit.category) {
            "Cannot subtract measurements of different categories"
        }
        val otherConverted = other.convertTo(unit)
        return Measurement(
            value = value - otherConverted.value,
            unit = unit,
            confidence = minOf(confidence, otherConverted.confidence)
        )
    }

    /**
     * Multiplicação por escalar
     */
    operator fun times(scalar: Double): Measurement {
        return copy(value = value * scalar)
    }

    /**
     * Divisão por escalar
     */
    operator fun div(scalar: Double): Measurement {
        require(scalar != 0.0) { "Cannot divide by zero" }
        return copy(value = value / scalar)
    }

    companion object {
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.7f

        /**
         * Cria medição a partir de valor e símbolo da unidade
         */
        fun fromString(valueStr: String, unitSymbol: String, confidence: Float = 1.0f): Measurement? {
            val value = valueStr.toDoubleOrNull() ?: return null
            val unit = MeasurementUnit.fromSymbol(unitSymbol) ?: return null

            return if (unit.isValidValue(value) && confidence.isValidConfidence()) {
                Measurement(value, unit, confidence)
            } else null
        }

        /**
         * Cria medição mock para testes
         */
        fun createMock(
            category: UnitCategory = UnitCategory.LENGTH,
            useMetric: Boolean = true
        ): Measurement {
            val units = if (useMetric) {
                MeasurementUnit.getMetricUnits(category)
            } else {
                MeasurementUnit.getImperialUnits(category)
            }

            val unit = units.randomOrNull() ?: MeasurementUnit.getDefaultForCategory(category)
            val value = when (category) {
                UnitCategory.LENGTH -> (1.0..100.0).random()
                UnitCategory.WEIGHT -> (0.1..10.0).random()
                UnitCategory.VOLUME -> (0.1..5.0).random()
                UnitCategory.AREA -> (0.1..10.0).random()
                UnitCategory.ANGLE -> (0.0..360.0).random()
                UnitCategory.TEMPERATURE -> (15.0..30.0).random()
            }

            return Measurement(value, unit, (0.7f..0.95f).random())
        }
    }
}

// ========== EXTENSION FUNCTIONS ==========

/**
 * Extension para criar medição rapidamente
 */
fun Double.withUnit(unit: MeasurementUnit, confidence: Float = 1.0f): Measurement {
    return Measurement(this, unit, confidence)
}

fun Int.withUnit(unit: MeasurementUnit, confidence: Float = 1.0f): Measurement {
    return Measurement(this.toDouble(), unit, confidence)
}

/**
 * Extension para listas de medições
 */
fun List<Measurement>.averageMeasurement(): Measurement? {
    if (isEmpty()) return null

    val firstUnit = first().unit
    require(all { it.unit.category == firstUnit.category }) {
        "All measurements must be of the same category"
    }

    // Converter todas para a mesma unidade
    val converted = map { it.convertTo(firstUnit) }
    val avgValue = converted.map { it.value }.average()
    val avgConfidence = converted.map { it.confidence }.average().toFloat()

    return Measurement(avgValue, firstUnit, avgConfidence)
}

/**
 * Filtra medições confiáveis
 */
fun List<Measurement>.filterReliable(threshold: Float = 0.7f): List<Measurement> {
    return filter { it.isReliable(threshold) }
}

/**
 * Encontra medição com maior confidence
 */
fun List<Measurement>.mostReliable(): Measurement? {
    return maxByOrNull { it.confidence }
}