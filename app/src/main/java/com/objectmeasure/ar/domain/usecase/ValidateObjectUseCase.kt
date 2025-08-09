package com.objectmeasure.ar.domain.usecase

import com.objectmeasure.ar.domain.model.DetectedObject
import com.objectmeasure.ar.domain.model.ObjectType
import com.objectmeasure.ar.domain.repository.BoundingBox
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Use Case para validar se um objeto detectado é adequado para medição
 * DIA 2: Primeiro use case implementando regras de negócio
 */
@Singleton
class ValidateObjectUseCase @Inject constructor() {

    /**
     * Valida se um objeto detectado pode ser medido com confiança
     */
    operator fun invoke( // REMOVIDO: suspend
        detectedObject: DetectedObject,
        boundingBox: BoundingBox
    ): ValidationResult {

        return when {
            // Validação 1: Confidence muito baixa
            !detectedObject.isReliableDetection() -> {
                ValidationResult.Invalid(
                    reason = "Confidence muito baixa: ${detectedObject.confidence}",
                    code = ValidationError.LOW_CONFIDENCE
                )
            }

            // Validação 2: BoundingBox inválido
            !boundingBox.isValid() -> {
                ValidationResult.Invalid(
                    reason = "BoundingBox inválido",
                    code = ValidationError.INVALID_BOUNDING_BOX
                )
            }

            // Validação 3: Objeto muito pequeno na tela
            boundingBox.area() < MIN_OBJECT_AREA -> {
                ValidationResult.Invalid(
                    reason = "Objeto muito pequeno para medição precisa",
                    code = ValidationError.OBJECT_TOO_SMALL
                )
            }

            // Validação 4: Tipo de objeto não suportado para altura
            detectedObject.type == ObjectType.UNKNOWN -> {
                ValidationResult.Invalid(
                    reason = "Tipo de objeto não identificado",
                    code = ValidationError.UNKNOWN_OBJECT_TYPE
                )
            }

            // Validação 5: Sucesso - objeto válido para medição
            else -> {
                ValidationResult.Valid(
                    confidence = detectedObject.confidence,
                    recommendedMeasurements = getRecommendedMeasurements(detectedObject.type)
                )
            }
        }
    }

    // ... resto do código igual

    private fun getRecommendedMeasurements(objectType: ObjectType): List<MeasurementType> {
        return when (objectType) {
            ObjectType.PERSON -> listOf(MeasurementType.HEIGHT, MeasurementType.DISTANCE)
            ObjectType.BOTTLE, ObjectType.CHAIR, ObjectType.TABLE -> listOf(
                MeasurementType.HEIGHT, MeasurementType.DISTANCE, MeasurementType.WEIGHT
            )
            ObjectType.PHONE, ObjectType.BOOK -> listOf(
                MeasurementType.HEIGHT, MeasurementType.DISTANCE
            )
            ObjectType.UNKNOWN -> emptyList()
        }
    }

    companion object {
        private const val MIN_OBJECT_AREA = 2500f
    }
}

// Enums iguais...
sealed class ValidationResult {
    data class Valid(
        val confidence: Float,
        val recommendedMeasurements: List<MeasurementType>
    ) : ValidationResult()

    data class Invalid(
        val reason: String,
        val code: ValidationError
    ) : ValidationResult()
}

enum class ValidationError {
    LOW_CONFIDENCE,
    INVALID_BOUNDING_BOX,
    OBJECT_TOO_SMALL,
    UNKNOWN_OBJECT_TYPE
}

enum class MeasurementType {
    HEIGHT,
    WEIGHT,
    DISTANCE,
    INCLINATION
}