package com.objectmeasure.ar.domain.model

import org.junit.Test
import org.junit.Assert.*

class ObjectMeasurementsTest {

    @Test
    fun `empty measurements should have no measurements`() {
        // When
        val measurements = ObjectMeasurements.empty()

        // Then
        assertFalse(measurements.hasAnyMeasurement())
        assertFalse(measurements.hasAllMeasurements())
        assertNull(measurements.height)
        assertNull(measurements.weight)
    }

    @Test
    fun `measurements with height should detect any measurement`() {
        // Given
        val height = Measurement(175.5f, MeasurementUnit.CENTIMETERS, 0.9f)

        // When
        val measurements = ObjectMeasurements(height, null, null, null)

        // Then
        assertTrue(measurements.hasAnyMeasurement())
        assertFalse(measurements.hasAllMeasurements())
    }

    @Test
    fun `measurement should format display correctly`() {
        // Given
        val measurement = Measurement(175.5f, MeasurementUnit.CENTIMETERS, 0.9f)

        // When
        val display = measurement.formatDisplay()

        // Then
        assertEquals("175,50 cm", display)
    }

    @Test
    fun `measurement should validate confidence correctly`() {
        // Given
        val highConfidence = Measurement(100.0f, MeasurementUnit.CENTIMETERS, 0.8f)
        val lowConfidence = Measurement(100.0f, MeasurementUnit.CENTIMETERS, 0.5f)

        // Then
        assertTrue(highConfidence.isReliable())
        assertFalse(lowConfidence.isReliable())
    }
}