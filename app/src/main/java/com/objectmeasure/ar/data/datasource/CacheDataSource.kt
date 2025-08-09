package com.objectmeasure.ar.data.datasource

import com.objectmeasure.ar.domain.model.DetectedObject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataSource para cache em memória de objetos detectados
 * DIA 3: Cache simples para iniciar data layer
 */
@Singleton
class CacheDataSource @Inject constructor() {

    private val _cachedObjects = MutableStateFlow<List<DetectedObject>>(emptyList())
    val cachedObjects: Flow<List<DetectedObject>> = _cachedObjects.asStateFlow()

    /**
     * Salva lista de objetos no cache
     */
    fun cacheObjects(objects: List<DetectedObject>) {
        _cachedObjects.value = objects
    }

    /**
     * Adiciona um objeto ao cache existente
     */
    fun addObject(detectedObject: DetectedObject) {
        val currentList = _cachedObjects.value.toMutableList()
        currentList.add(detectedObject)

        // Manter apenas os últimos 10 objetos
        if (currentList.size > MAX_CACHE_SIZE) {
            currentList.removeAt(0)
        }

        _cachedObjects.value = currentList
    }

    /**
     * Obtém objetos do cache
     */
    fun getCachedObjects(): List<DetectedObject> {
        return _cachedObjects.value
    }

    /**
     * Limpa o cache
     */
    fun clearCache() {
        _cachedObjects.value = emptyList()
    }

    /**
     * Obtém histórico limitado de objetos
     */
    fun getHistory(limit: Int): List<DetectedObject> {
        return _cachedObjects.value.takeLast(limit)
    }

    companion object {
        // Ajustado para refletir o comentário na função addObject
        private const val MAX_CACHE_SIZE = 10
    }
}