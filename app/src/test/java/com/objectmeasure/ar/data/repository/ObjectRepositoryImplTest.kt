package com.objectmeasure.ar.data.repository

import com.objectmeasure.ar.data.datasource.CacheDataSource
import com.objectmeasure.ar.domain.model.DetectedObject
import com.objectmeasure.ar.domain.model.ObjectMeasurements
import com.objectmeasure.ar.domain.model.ObjectType
import com.objectmeasure.ar.domain.repository.BoundingBox
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

class ObjectRepositoryImplTest {

    private val cacheDataSource = CacheDataSource()
    private val repository = ObjectRepositoryImpl(cacheDataSource)

    @Test
    fun `should return AR as available`() = runTest {
        // When
        val result = repository.isARAvailable()

        // Then
        assertTrue("AR should be available in mock", result)
    }

    @Test
    fun `should save detected object to cache`() = runTest {
        // Given
        val detectedObject = DetectedObject(
            type = ObjectType.PERSON,
            measurements = ObjectMeasurements.empty(),
            confidence = 0.9f
        )

        // When
        val result = repository.saveDetectedObject(detectedObject)

        // Then
        assertTrue("Should save successfully", result.isSuccess)

        // Verify it's in cache
        val history = repository.getDetectionHistory(10)
        assertTrue("History should contain object", history.isSuccess)
        assertEquals(1, history.getOrNull()?.size)
    }

    @Test
    fun `should measure object and save to cache`() = runTest {
        // Given
        val boundingBox = BoundingBox(100f, 100f, 200f, 200f)

        // When
        val result = repository.measureObject(boundingBox, ObjectType.BOTTLE)

        // Then
        assertTrue("Measurement should succeed", result.isSuccess)

        val measuredObject = result.getOrNull()
        assertNotNull("Should return measured object", measuredObject)
        assertEquals(ObjectType.BOTTLE, measuredObject?.type)
    }

    @Test
    fun `should return empty list for detect objects initially`() = runTest {
        // When
        repository.detectObjects(ByteArray(0)).collect { result ->
            // Then
            assertTrue("Should succeed", result.isSuccess)
            val objects = result.getOrNull()
            assertNotNull("Should return list", objects)
            assertTrue("Should be empty initially", objects!!.isEmpty())
        }
    }
}