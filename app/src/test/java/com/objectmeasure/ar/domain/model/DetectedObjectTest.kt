package com.objectmeasure.ar.domain.model

import org.junit.Test
import org.junit.Assert.*

class DetectedObjectTest {

    @Test
    fun `detected object should have valid id and timestamp`() {
        // Given
        val measurements = ObjectMeasurements.empty()

        // When
        val obj = DetectedObject(
            type = ObjectType.PERSON,
            measurements = measurements,
            confidence = 0.9f
        )

        // Then
        assertNotNull(obj.id)
        assertTrue(obj.id.isNotEmpty())
        assertTrue(obj.timestamp > 0)
    }

    @Test
    fun `high confidence detection should be reliable`() {
        // Given
        val measurements = ObjectMeasurements.empty()
        val highConfidence = DetectedObject(
            type = ObjectType.PERSON,
            measurements = measurements,
            confidence = 0.8f
        )
        val lowConfidence = DetectedObject(
            type = ObjectType.PERSON,
            measurements = measurements,
            confidence = 0.5f
        )

        // Then
        assertTrue(highConfidence.isReliableDetection())
        assertFalse(lowConfidence.isReliableDetection())
    }

    @Test
    fun `object with measurements should have useful measurements`() {
        // Given
        val height = Measurement(180.0f, MeasurementUnit.CENTIMETERS)
        val measurements = ObjectMeasurements(height, null, null, null)

        // When
        val obj = DetectedObject(
            type = ObjectType.PERSON,
            measurements = measurements,
            confidence = 0.9f
        )

        // Then
        assertTrue(obj.hasUsefulMeasurements())
        assertEquals("Pessoa", obj.getDisplayName())
    }

    @Test
    fun `ObjectType should convert from string correctly`() {
        // When & Then
        assertEquals(ObjectType.PERSON, ObjectType.fromString("PERSON"))
        assertEquals(ObjectType.PERSON, ObjectType.fromString("person"))
        assertEquals(ObjectType.UNKNOWN, ObjectType.fromString("invalid"))
    }

    @Test
    fun `height measurable types should be correct`() {
        // When
        val heightMeasurable = ObjectType.getHeightMeasurableTypes()

        // Then
        assertTrue(heightMeasurable.contains(ObjectType.PERSON))
        assertTrue(heightMeasurable.contains(ObjectType.BOTTLE))
        assertFalse(heightMeasurable.contains(ObjectType.PHONE))
    }
}