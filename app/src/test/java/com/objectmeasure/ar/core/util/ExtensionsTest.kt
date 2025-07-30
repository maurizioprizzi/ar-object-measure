package com.objectmeasure.ar.core.util

import org.junit.Test
import org.junit.Assert.*

/**
 * Testes das extensões básicas - DIA 1
 * Validando nossa estrutura Clean Architecture
 */
class ExtensionsTest {

    @Test
    fun `isNotNullOrEmpty deve retornar true para string válida`() {
        // Given (Arrange)
        val validString = "Hello World"

        // When (Act)
        val result = validString.isNotNullOrEmpty()

        // Then (Assert)
        assertTrue("String válida deve retornar true", result)
    }

    @Test
    fun `isNotNullOrEmpty deve retornar false para string vazia`() {
        // Given
        val emptyString = ""

        // When
        val result = emptyString.isNotNullOrEmpty()

        // Then
        assertFalse("String vazia deve retornar false", result)
    }

    @Test
    fun `isNotNullOrEmpty deve retornar false para string null`() {
        // Given
        val nullString: String? = null

        // When
        val result = nullString.isNotNullOrEmpty()

        // Then
        assertFalse("String null deve retornar false", result)
    }

    @Test
    fun `isInRange deve retornar true para número dentro do intervalo`() {
        // Given
        val number = 5.0f
        val min = 1.0f
        val max = 10.0f

        // When
        val result = number.isInRange(min, max)

        // Then
        assertTrue("Número dentro do range deve retornar true", result)
    }

    @Test
    fun `isInRange deve retornar false para número fora do intervalo`() {
        // Given
        val number = 15.0f
        val min = 1.0f
        val max = 10.0f

        // When
        val result = number.isInRange(min, max)

        // Then
        assertFalse("Número fora do range deve retornar false", result)
    }
}