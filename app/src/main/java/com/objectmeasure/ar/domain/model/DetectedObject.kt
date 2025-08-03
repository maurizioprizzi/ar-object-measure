package com.objectmeasure.ar.domain.model

import java.util.UUID

/**
 * Representa um objeto detectado pela câmera AR
 * DIA 2: Entidade principal que une detecção + medições
 */
data class DetectedObject(
    val id: String = UUID.randomUUID().toString(),
    val type: ObjectType,
    val measurements: ObjectMeasurements,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
) {
    init {
        require(confidence in 0.0f..1.0f) { "Confidence must be between 0.0 and 1.0" }
    }

    /**
     * Verifica se a detecção é confiável
     */
    fun isReliableDetection(): Boolean {
        return confidence > 0.7f
    }

    /**
     * Verifica se o objeto tem medições úteis
     */
    fun hasUsefulMeasurements(): Boolean {
        return measurements.hasAnyMeasurement()
    }

    /**
     * Retorna uma descrição do objeto para display
     */
    fun getDisplayName(): String {
        return type.displayName
    }
}

/**
 * Tipos de objetos que podem ser detectados
 * DIA 2: Começando com tipos básicos
 */
enum class ObjectType(val displayName: String) {
    PERSON("Pessoa"),
    BOTTLE("Garrafa"),
    PHONE("Celular"),
    BOOK("Livro"),
    CHAIR("Cadeira"),
    TABLE("Mesa"),
    UNKNOWN("Objeto Desconhecido");

    companion object {
        /**
         * Converte string para ObjectType, com fallback para UNKNOWN
         */
        fun fromString(value: String): ObjectType {
            return values().find {
                it.name.equals(value, ignoreCase = true)
            } ?: UNKNOWN
        }

        /**
         * Retorna tipos que suportam medição de altura
         */
        fun getHeightMeasurableTypes(): List<ObjectType> {
            return listOf(PERSON, BOTTLE, CHAIR, TABLE)
        }
    }
}