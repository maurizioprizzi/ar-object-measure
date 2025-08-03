package com.objectmeasure.ar.domain.usecase

import com.objectmeasure.ar.domain.model.DetectedObject
import com.objectmeasure.ar.domain.model.ObjectMeasurements
import com.objectmeasure.ar.domain.model.ObjectType
import com.objectmeasure.ar.domain.repository.BoundingBox
import org.junit.Test
import org.junit.Assert.*

class ValidateObjectUseCaseTest {

    private val useCase = ValidateObjectUseCase()

    @Test
    fun `should return Valid for good object and bounding box`() { // REMOVIDO: runTest
        // Given
        val detectedObject = DetectedObject(
            type = ObjectType.PERSON,
            measurements = ObjectMeasurements.empty(),
            confidence = 0.9f
        )
        val boundingBox = BoundingBox(100f, 100f, 200f, 200f)

        // When
        val result = useCase(detectedObject, boundingBox)

        // Then
        assertTrue(result is ValidationResult.Valid)
        val validResult = result as ValidationResult.Valid
        assertEquals(0.9f, validResult.confidence, 0.01f)
    }

    @Test
    fun `should return Invalid for low confidence object`() {
        // Given
        val detectedObject = DetectedObject(
            type = ObjectType.PERSON,
            measurements = ObjectMeasurements.empty(),
            confidence = 0.5f
        )
        val boundingBox = BoundingBox(100f, 100f, 200f, 200f)

        // When
        val result = useCase(detectedObject, boundingBox)

        // Then
        assertTrue(result is ValidationResult.Invalid)
        val invalidResult = result as ValidationResult.Invalid
        assertEquals(ValidationError.LOW_CONFIDENCE, invalidResult.code)
    }

    @Test
    fun `should return Invalid for small object`() {
        // Given
        val detectedObject = DetectedObject(
            type = ObjectType.PERSON,
            measurements = ObjectMeasurements.empty(),
            confidence = 0.9f
        )
        val boundingBox = BoundingBox(0f, 0f, 10f, 10f)

        // When
        val result = useCase(detectedObject, boundingBox)

        // Then
        assertTrue(result is ValidationResult.Invalid)
        val invalidResult = result as ValidationResult.Invalid
        assertEquals(ValidationError.OBJECT_TOO_SMALL, invalidResult.code)
    }
}