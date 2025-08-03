package com.objectmeasure.ar.domain.model

/**
 * Unidades de medida suportadas pela aplicação
 * DIA 2: Começando com unidades básicas
 */
enum class MeasurementUnit(val symbol: String, val displayName: String) {
    // Comprimento
    CENTIMETERS("cm", "Centímetros"),
    METERS("m", "Metros"),

    // Peso
    GRAMS("g", "Gramas"),
    KILOGRAMS("kg", "Quilogramas"),

    // Ângulo
    DEGREES("°", "Graus");

    companion object {
        fun getDefaultForHeight() = CENTIMETERS
        fun getDefaultForWeight() = KILOGRAMS
        fun getDefaultForDistance() = METERS
        fun getDefaultForAngle() = DEGREES
    }
}