package com.objectmeasure.ar.data.datasource

import com.objectmeasure.ar.domain.model.DetectedObject
import com.objectmeasure.ar.domain.model.ObjectMeasurements
import com.objectmeasure.ar.domain.model.ObjectType
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Assert.*

class CacheDataSourceTest {

    private val cacheDataSource = CacheDataSource()

    @Test
    fun `should start with empty cache`() = runTest {
        // When
        val result = cacheDataSource.getCachedObjects()

        // Then
        assertTrue(result.isEmpty())
    }

    @Test
    fun `should cache objects correctly`() = runTest {
        // Given
        val objects = listOf(
            DetectedObject(
                type = ObjectType.PERSON,
                measurements = ObjectMeasurements.empty(),
                confidence = 0.9f
            )
        )

        // When
        cacheDataSource.cacheObjects(objects)
        val result = cacheDataSource.getCachedObjects()

        // Then
        assertEquals(1, result.size)
        assertEquals(ObjectType.PERSON, result.first().type)
    }

    @Test
    fun `should add objects to existing cache`() = runTest {
        // Given
        val object1 = DetectedObject(
            type = ObjectType.PERSON,
            measurements = ObjectMeasurements.empty(),
            confidence = 0.9f
        )
        val object2 = DetectedObject(
            type = ObjectType.BOTTLE,
            measurements = ObjectMeasurements.empty(),
            confidence = 0.8f
        )

        // When
        cacheDataSource.addObject(object1)
        cacheDataSource.addObject(object2)
        val result = cacheDataSource.getCachedObjects()

        // Then
        assertEquals(2, result.size)
    }
}