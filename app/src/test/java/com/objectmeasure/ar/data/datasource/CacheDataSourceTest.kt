package com.objectmeasure.ar.data.datasource

import com.objectmeasure.ar.domain.model.DetectedObject
import com.objectmeasure.ar.domain.model.ObjectMeasurements
import com.objectmeasure.ar.domain.model.ObjectType
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Testes para o CacheDataSource.
 * Versão melhorada cobrindo todos os cenários, incluindo casos de borda.
 */
class CacheDataSourceTest {

    // Instância a ser testada.
    private lateinit var cacheDataSource: CacheDataSource

    // Anotação @Before faz este método rodar antes de CADA teste.
    // Isso garante que cada teste comece com um cache limpo e independente.
    @Before
    fun setUp() {
        cacheDataSource = CacheDataSource()
    }

    @Test
    fun `should start with empty cache`() {
        // When
        val result = cacheDataSource.getCachedObjects()

        // Then
        assertTrue("O cache deve iniciar vazio", result.isEmpty())
    }

    @Test
    fun `should cache a list of objects correctly`() {
        // Given
        val objects = listOf(createTestObject(ObjectType.PERSON))

        // When
        cacheDataSource.cacheObjects(objects)
        val result = cacheDataSource.getCachedObjects()

        // Then
        assertEquals("Deve haver 1 objeto no cache", 1, result.size)
        assertEquals("O tipo do objeto deve ser PERSON", ObjectType.PERSON, result.first().type)
    }

    @Test
    fun `should add objects to existing cache`() {
        // Given
        val object1 = createTestObject(ObjectType.PERSON)
        val object2 = createTestObject(ObjectType.BOTTLE)

        // When
        cacheDataSource.addObject(object1)
        cacheDataSource.addObject(object2)
        val result = cacheDataSource.getCachedObjects()

        // Then
        assertEquals("Deve haver 2 objetos no cache", 2, result.size)
    }

    @Test
    fun `should clear cache correctly`() {
        // Given
        cacheDataSource.addObject(createTestObject(ObjectType.CHAIR))

        // When
        cacheDataSource.clearCache()
        val result = cacheDataSource.getCachedObjects()

        // Then
        assertTrue("O cache deve estar vazio após a limpeza", result.isEmpty())
    }

    @Test
    fun `should respect MAX_CACHE_SIZE limit`() {
        // Given
        // Adiciona 10 objetos (o limite máximo)
        for (i in 1..10) {
            cacheDataSource.addObject(createTestObject(ObjectType.BOOK, id = i.toString()))
        }

        // When
        // Adiciona o 11º objeto, que deve remover o primeiro
        val eleventhObject = createTestObject(ObjectType.PHONE, id = "11")
        cacheDataSource.addObject(eleventhObject)

        val result = cacheDataSource.getCachedObjects()

        // Then
        assertEquals("O tamanho do cache deve permanecer no limite de 10", 10, result.size)
        assertEquals("O primeiro objeto da lista deve ser o de id=2", "2", result.first().id)
        assertEquals("O último objeto da lista deve ser o 11º adicionado", eleventhObject, result.last())
    }

    @Test
    fun `should get history with correct limit`() {
        // Given
        // Adiciona 5 objetos
        for (i in 1..5) {
            cacheDataSource.addObject(createTestObject(ObjectType.TABLE, id = i.toString()))
        }

        // When
        val history = cacheDataSource.getHistory(limit = 3)

        // Then
        assertEquals("O histórico deve ter o tamanho do limite solicitado", 3, history.size)
        assertEquals("O primeiro item do histórico deve ser o objeto de id=3", "3", history.first().id)
        assertEquals("O último item do histórico deve ser o objeto de id=5", "5", history.last().id)
    }

    /**
     * Função helper para criar objetos de teste facilmente e evitar duplicação de código.
     */
    private fun createTestObject(type: ObjectType, id: String = "test-id"): DetectedObject {
        return DetectedObject(
            id = id,
            type = type,
            measurements = ObjectMeasurements.empty(),
            confidence = 0.9f
        )
    }
}