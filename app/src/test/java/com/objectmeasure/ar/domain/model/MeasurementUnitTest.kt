package com.objectmeasure.ar.domain.model

import org.junit.Test
import org.junit.Assert.*

/**
 * Testes para unidades de medida - DIA 2
 * Validando nossos enums e funções companion
 */
class MeasurementUnitTest {

    @Test
    fun `getDefaultForHeight deve retornar CENTIMETERS`() {
        // When
        val result = MeasurementUnit.getDefaultForHeight()

        // Then
        assertEquals(MeasurementUnit.CENTIMETERS, result)
        assertEquals("cm", result.symbol)
    }

    @Test
    fun `getDefaultForWeight deve retornar KILOGRAMS`() {
        // When
        val result = MeasurementUnit.getDefaultForWeight()

        // Then
        assertEquals(MeasurementUnit.KILOGRAMS, result)
        assertEquals("kg", result.symbol)
    }

    @Test
    fun `enum deve ter símbolos corretos`() {
        // Then
        assertEquals("cm", MeasurementUnit.CENTIMETERS.symbol)
        assertEquals("m", MeasurementUnit.METERS.symbol)
        assertEquals("g", MeasurementUnit.GRAMS.symbol)
        assertEquals("kg", MeasurementUnit.KILOGRAMS.symbol)
        assertEquals("°", MeasurementUnit.DEGREES.symbol)
    }
}