package com.objectmeasure.ar.domain.model

/**
 * Representa as medições de um objeto detectado
 * DIA 2: Estrutura básica para altura, peso, distância e ângulo
 */
data class ObjectMeasurements(
    val height: Measurement?,
    val weight: Measurement?,
    val distance: Measurement?,
    val inclination: Measurement?
) {
    /**
     * Verifica se pelo menos uma medição está disponível
     */
    fun hasAnyMeasurement(): Boolean {
        return height != null || weight != null || distance != null || inclination != null
    }

    /**
     * Verifica se todas as medições estão disponíveis
     */
    fun hasAllMeasurements(): Boolean {
        return height != null && weight != null && distance != null && inclination != null
    }

    companion object {
        /**
         * Cria ObjectMeasurements vazio
         */
        fun empty() = ObjectMeasurements(
            height = null,
            weight = null,
            distance = null,
            inclination = null
        )
    }
}

/**
 * Representa uma medição individual com valor, unidade e confiança
 */
data class Measurement(
    val value: Float,
    val unit: MeasurementUnit,
    val confidence: Float = 1.0f
) {
    init {
        require(value >= 0) { "Measurement value must be positive" }
        require(confidence in 0.0f..1.0f) { "Confidence must be between 0.0 and 1.0" }
    }

    /**
     * Retorna a medição formatada como string
     */
    fun formatDisplay(): String {
        return "%.2f %s".format(value, unit.symbol)
    }

    /**
     * Verifica se a medição é confiável (confidence > 0.7)
     */
    fun isReliable(): Boolean {
        return confidence > 0.7f
    }
}