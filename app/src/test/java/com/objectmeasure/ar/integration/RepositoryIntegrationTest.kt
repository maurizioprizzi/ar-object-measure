package com.objectmeasure.ar.integration

import com.objectmeasure.ar.data.datasource.CacheDataSource
import com.objectmeasure.ar.data.repository.ObjectRepositoryImpl
import com.objectmeasure.ar.domain.model.ObjectType
import com.objectmeasure.ar.domain.repository.BoundingBox
import com.objectmeasure.ar.domain.usecase.ValidateObjectUseCase
import com.objectmeasure.ar.domain.usecase.ValidationResult
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

/**
 * Teste de integração - Domain + Data layers
 * DIA 3: Verificando se as camadas trabalham juntas
 */
class RepositoryIntegrationTest {

    private val cacheDataSource = CacheDataSource()
    private val repository = ObjectRepositoryImpl(cacheDataSource)
    private val validateUseCase = ValidateObjectUseCase()

    @Test
    fun `should integrate repository with cache successfully`() = runTest {
        // Given
        val boundingBox = BoundingBox(100f, 100f, 200f, 200f)

        // When - usar repository para medir objeto
        val measureResult = repository.measureObject(boundingBox, ObjectType.PERSON)

        // Then - verificar se foi salvo e pode ser validado
        assertTrue("Measurement should succeed", measureResult.isSuccess)

        val detectedObject = measureResult.getOrNull()!!
        val validationResult = validateUseCase(detectedObject, boundingBox)

        assertTrue("Validation should pass", validationResult is ValidationResult.Valid)
    }

    @Test
    fun `should save and retrieve from history`() = runTest {
        // Given
        val boundingBox = BoundingBox(150f, 150f, 250f, 250f)

        // When
        repository.measureObject(boundingBox, ObjectType.BOTTLE)
        repository.measureObject(boundingBox, ObjectType.PHONE)

        val historyResult = repository.getDetectionHistory(5)

        // Then
        assertTrue("History should be available", historyResult.isSuccess)
        val history = historyResult.getOrNull()!!
        assertEquals("Should have 2 objects", 2, history.size)
    }
}