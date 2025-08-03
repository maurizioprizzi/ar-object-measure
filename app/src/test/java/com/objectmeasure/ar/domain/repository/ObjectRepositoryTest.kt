package com.objectmeasure.ar.domain.repository

import org.junit.Test
import org.junit.Assert.*

/**
 * Testes para estruturas do Repository
 * DIA 2: Testando BoundingBox e validações
 */
class ObjectRepositoryTest {

    @Test
    fun `BoundingBox should calculate dimensions correctly`() {
        // Given
        val bbox = BoundingBox(left = 100f, top = 50f, right = 200f, bottom = 150f)

        // When & Then
        assertEquals(100f, bbox.width(), 0.01f)
        assertEquals(100f, bbox.height(), 0.01f)
        assertEquals(10000f, bbox.area(), 0.01f)
    }

    @Test
    fun `BoundingBox should validate correctly`() {
        // Given
        val validBox = BoundingBox(100f, 50f, 200f, 150f)
        val invalidBox = BoundingBox(200f, 150f, 100f, 50f) // inverted
        val negativeBox = BoundingBox(-10f, -5f, 100f, 50f)

        // Then
        assertTrue(validBox.isValid())
        assertFalse(invalidBox.isValid())
        assertFalse(negativeBox.isValid())
    }

    @Test
    fun `BoundingBox should calculate center correctly`() {
        // Given
        val bbox = BoundingBox(100f, 50f, 200f, 150f)

        // When
        val center = bbox.center()

        // Then
        assertEquals(150f, center.first, 0.01f)  // center X
        assertEquals(100f, center.second, 0.01f) // center Y
    }

    @Test
    fun `BoundingBox edge cases should work`() {
        // Given
        val zeroBox = BoundingBox(0f, 0f, 100f, 100f)

        // When & Then
        assertTrue(zeroBox.isValid())
        assertEquals(100f, zeroBox.width(), 0.01f)
        assertEquals(100f, zeroBox.height(), 0.01f)
        assertEquals(Pair(50f, 50f), zeroBox.center())
    }
}