package com.objectmeasure.ar.data.datasource

import com.objectmeasure.ar.core.util.filterOutliers
import com.objectmeasure.ar.core.util.isHighConfidence
import com.objectmeasure.ar.core.util.logDebug
import com.objectmeasure.ar.domain.model.DetectedObject
import com.objectmeasure.ar.domain.model.ObjectType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * DataSource para cache em memória de objetos detectados - DIA 3 REVISADO
 * Cache thread-safe com funcionalidades específicas para AR
 */
@Singleton
class CacheDataSource @Inject constructor() {

    private val mutex = Mutex()

    private val _cachedObjects = MutableStateFlow<List<DetectedObject>>(emptyList())
    val cachedObjects: Flow<List<DetectedObject>> = _cachedObjects.asStateFlow()

    private var maxCacheSize = DEFAULT_MAX_CACHE_SIZE
    private var autoCleanupEnabled = true
    private var confidenceThreshold = DEFAULT_CONFIDENCE_THRESHOLD

    // ========== OPERAÇÕES BÁSICAS ==========

    /**
     * Salva lista de objetos no cache (thread-safe)
     */
    suspend fun cacheObjects(objects: List<DetectedObject>) = mutex.withLock {
        val validObjects = objects.filter { it.confidence.isHighConfidence(confidenceThreshold) }
        _cachedObjects.value = validObjects.takeLast(maxCacheSize)

        validObjects.size.logDebug("Cached objects")
    }

    /**
     * Adiciona um objeto ao cache existente (thread-safe e otimizada)
     */
    suspend fun addObject(detectedObject: DetectedObject) = mutex.withLock {
        // Validar confidence mínima
        if (!detectedObject.confidence.isHighConfidence(confidenceThreshold)) {
            "Object filtered out due to low confidence: ${detectedObject.confidence}".logDebug()
            return@withLock
        }

        // Verificar se já existe objeto similar (evita duplicatas)
        val existingIndex = _cachedObjects.value.indexOfFirst {
            it.id == detectedObject.id || areSimilarObjects(it, detectedObject)
        }

        val updatedList = if (existingIndex >= 0) {
            // Atualizar objeto existente
            _cachedObjects.value.toMutableList().apply {
                this[existingIndex] = detectedObject
            }
        } else {
            // Adicionar novo objeto
            (_cachedObjects.value + detectedObject).takeLast(maxCacheSize)
        }

        _cachedObjects.value = updatedList

        if (autoCleanupEnabled) {
            performAutoCleanup()
        }
    }

    /**
     * Obtém objetos do cache (imutável)
     */
    fun getCachedObjects(): List<DetectedObject> {
        return _cachedObjects.value.toList()
    }

    /**
     * Limpa o cache completamente
     */
    suspend fun clearCache() = mutex.withLock {
        _cachedObjects.value = emptyList()
        "Cache cleared".logDebug("Cache")
    }

    // ========== OPERAÇÕES AVANÇADAS ==========

    /**
     * Remove objetos por ID
     */
    suspend fun removeObject(objectId: String) = mutex.withLock {
        _cachedObjects.value = _cachedObjects.value.filterNot { it.id == objectId }
    }

    /**
     * Remove múltiplos objetos por IDs
     */
    suspend fun removeObjects(objectIds: List<String>) = mutex.withLock {
        val idsSet = objectIds.toSet()
        _cachedObjects.value = _cachedObjects.value.filterNot { it.id in idsSet }
    }

    /**
     * Atualiza objeto existente
     */
    suspend fun updateObject(updatedObject: DetectedObject) = mutex.withLock {
        _cachedObjects.value = _cachedObjects.value.map { obj ->
            if (obj.id == updatedObject.id) updatedObject else obj
        }
    }

    /**
     * Substitui objeto existente ou adiciona se não existir
     */
    suspend fun upsertObject(detectedObject: DetectedObject) = mutex.withLock {
        val existingIndex = _cachedObjects.value.indexOfFirst { it.id == detectedObject.id }

        _cachedObjects.value = if (existingIndex >= 0) {
            _cachedObjects.value.toMutableList().apply {
                this[existingIndex] = detectedObject
            }
        } else {
            (_cachedObjects.value + detectedObject).takeLast(maxCacheSize)
        }
    }

    // ========== CONSULTAS ESPECÍFICAS PARA AR ==========

    /**
     * Busca objetos por proximidade 3D (útil para AR)
     */
    fun getObjectsNearPosition(
        x: Float,
        y: Float,
        z: Float,
        radius: Float
    ): Flow<List<DetectedObject>> {
        return _cachedObjects.map { objects ->
            objects.filter { obj ->
                obj.position?.let { pos ->
                    val distance = sqrt(
                        (pos.x - x).pow(2) +
                                (pos.y - y).pow(2) +
                                (pos.z - z).pow(2)
                    )
                    distance <= radius
                } ?: false
            }
        }
    }

    /**
     * Filtra por tipo de objeto
     */
    fun getObjectsByType(type: ObjectType): Flow<List<DetectedObject>> {
        return _cachedObjects.map { objects ->
            objects.filter { it.type == type }
        }
    }

    /**
     * Filtra por múltiplos tipos
     */
    fun getObjectsByTypes(types: List<ObjectType>): Flow<List<DetectedObject>> {
        return _cachedObjects.map { objects ->
            objects.filter { it.type in types }
        }
    }

    /**
     * Obtém objetos com confidence alta
     */
    fun getHighConfidenceObjects(threshold: Float = 0.8f): Flow<List<DetectedObject>> {
        return _cachedObjects.map { objects ->
            objects.filter { it.confidence >= threshold }
        }
    }

    /**
     * Obtém objetos detectados recentemente
     */
    fun getRecentObjects(maxAgeMs: Long = 30_000): Flow<List<DetectedObject>> {
        return _cachedObjects.map { objects ->
            val cutoffTime = System.currentTimeMillis() - maxAgeMs
            objects.filter { it.timestamp >= cutoffTime }
        }
    }

    /**
     * Obtém histórico limitado de objetos
     */
    fun getHistory(limit: Int): List<DetectedObject> {
        return _cachedObjects.value.takeLast(limit)
    }

    /**
     * Obtém histórico filtrado
     */
    fun getFilteredHistory(
        objectTypes: List<ObjectType>? = null,
        minConfidence: Float = 0.0f,
        since: Long? = null,
        limit: Int = 10
    ): List<DetectedObject> {
        return _cachedObjects.value
            .let { objects ->
                if (objectTypes != null) {
                    objects.filter { it.type in objectTypes }
                } else objects
            }
            .filter { it.confidence >= minConfidence }
            .let { objects ->
                if (since != null) {
                    objects.filter { it.timestamp >= since }
                } else objects
            }
            .takeLast(limit)
    }

    // ========== ESTATÍSTICAS E ANÁLISES ==========

    /**
     * Obtém estatísticas do cache
     */
    fun getCacheStatistics(): CacheStatistics {
        val objects = _cachedObjects.value
        val confidences = objects.map { it.confidence }

        return CacheStatistics(
            totalObjects = objects.size,
            reliableObjects = objects.count { it.isReliableDetection() },
            averageConfidence = confidences.averageOrNull()?.toFloat() ?: 0f,
            maxConfidence = confidences.maxOrNull() ?: 0f,
            minConfidence = confidences.minOrNull() ?: 0f,
            typeDistribution = objects.groupingBy { it.type }.eachCount(),
            oldestTimestamp = objects.minOfOrNull { it.timestamp } ?: 0L,
            newestTimestamp = objects.maxOfOrNull { it.timestamp } ?: 0L,
            cacheUtilization = objects.size.toFloat() / maxCacheSize
        )
    }

    /**
     * Obtém objetos agrupados por tipo
     */
    fun getObjectsGroupedByType(): Flow<Map<ObjectType, List<DetectedObject>>> {
        return _cachedObjects.map { objects ->
            objects.groupBy { it.type }
        }
    }

    /**
     * Obtém distribuição de confidence
     */
    fun getConfidenceDistribution(): Map<String, Int> {
        val objects = _cachedObjects.value
        return mapOf(
            "Very High (>0.9)" to objects.count { it.confidence > 0.9f },
            "High (0.8-0.9)" to objects.count { it.confidence in 0.8f..0.9f },
            "Medium (0.7-0.8)" to objects.count { it.confidence in 0.7f..0.8f },
            "Low (<0.7)" to objects.count { it.confidence < 0.7f }
        )
    }

    // ========== CONFIGURAÇÕES ==========

    /**
     * Define tamanho máximo do cache
     */
    suspend fun setMaxCacheSize(newSize: Int) = mutex.withLock {
        require(newSize > 0) { "Cache size must be positive" }
        maxCacheSize = newSize

        // Ajustar cache atual se necessário
        if (_cachedObjects.value.size > newSize) {
            _cachedObjects.value = _cachedObjects.value.takeLast(newSize)
        }
    }

    /**
     * Define threshold mínimo de confidence
     */
    fun setConfidenceThreshold(threshold: Float) {
        require(threshold in 0.0f..1.0f) { "Confidence threshold must be between 0.0 and 1.0" }
        confidenceThreshold = threshold
    }

    /**
     * Habilita/desabilita limpeza automática
     */
    fun setAutoCleanupEnabled(enabled: Boolean) {
        autoCleanupEnabled = enabled
    }

    /**
     * Obtém configurações atuais do cache
     */
    fun getCacheConfiguration(): CacheConfiguration {
        return CacheConfiguration(
            maxSize = maxCacheSize,
            autoCleanupEnabled = autoCleanupEnabled,
            confidenceThreshold = confidenceThreshold,
            currentSize = _cachedObjects.value.size
        )
    }

    // ========== OPERAÇÕES DE LIMPEZA ==========

    /**
     * Remove objetos antigos baseado em timestamp
     */
    suspend fun removeOldObjects(maxAgeMs: Long) = mutex.withLock {
        val cutoffTime = System.currentTimeMillis() - maxAgeMs
        val sizeBefore = _cachedObjects.value.size

        _cachedObjects.value = _cachedObjects.value.filter {
            it.timestamp >= cutoffTime
        }

        val removedCount = sizeBefore - _cachedObjects.value.size
        removedCount.logDebug("Removed old objects", "AutoCleanup")
    }

    /**
     * Remove objetos com confidence baixa
     */
    suspend fun removeLowConfidenceObjects(threshold: Float = 0.5f) = mutex.withLock {
        val sizeBefore = _cachedObjects.value.size

        _cachedObjects.value = _cachedObjects.value.filter {
            it.confidence >= threshold
        }

        val removedCount = sizeBefore - _cachedObjects.value.size
        removedCount.logDebug("Removed low confidence objects", "ConfidenceFilter")
    }

    /**
     * Remove outliers baseado em confidence (usando extensão)
     */
    suspend fun removeConfidenceOutliers() = mutex.withLock {
        val objects = _cachedObjects.value
        if (objects.size < 3) return@withLock

        val confidences = objects.map { it.confidence }
        val filteredConfidences = confidences.filterOutliers()
        val validConfidenceSet = filteredConfidences.toSet()

        val sizeBefore = objects.size
        _cachedObjects.value = objects.filter {
            it.confidence in validConfidenceSet
        }

        val removedCount = sizeBefore - _cachedObjects.value.size
        removedCount.logDebug("Removed confidence outliers")
    }

    /**
     * Remove objetos duplicados por similaridade
     */
    suspend fun removeDuplicates() = mutex.withLock {
        val objects = _cachedObjects.value
        val uniqueObjects = mutableListOf<DetectedObject>()

        for (obj in objects) {
            val isDuplicate = uniqueObjects.any { existing ->
                areSimilarObjects(existing, obj)
            }

            if (!isDuplicate) {
                uniqueObjects.add(obj)
            }
        }

        val removedCount = objects.size - uniqueObjects.size
        _cachedObjects.value = uniqueObjects

        removedCount.logDebug("Removed duplicate objects")
    }

    /**
     * Compacta cache removendo objetos menos relevantes
     */
    suspend fun compactCache(targetSize: Int = maxCacheSize / 2) = mutex.withLock {
        val objects = _cachedObjects.value
        if (objects.size <= targetSize) return@withLock

        // Manter objetos com maior confidence e mais recentes
        val sortedObjects = objects.sortedWith(
            compareByDescending<DetectedObject> { it.confidence }
                .thenByDescending { it.timestamp }
        )

        val sizeBefore = objects.size
        _cachedObjects.value = sortedObjects.take(targetSize)

        val removedCount = sizeBefore - _cachedObjects.value.size
        removedCount.logDebug("Compacted cache")
    }

    // ========== OPERAÇÕES PRIVADAS ==========

    /**
     * Limpeza automática (remove objetos antigos e duplicados)
     */
    private suspend fun performAutoCleanup() {
        // Remove objetos muito antigos (mais de 5 minutos)
        removeOldObjects(5 * 60 * 1000)

        // Remove objetos com confidence muito baixa
        removeLowConfidenceObjects(0.3f)

        // Se cache está cheio, compactar
        if (_cachedObjects.value.size >= maxCacheSize) {
            compactCache()
        }
    }

    /**
     * Verifica se dois objetos são similares (para evitar duplicatas)
     */
    private fun areSimilarObjects(
        obj1: DetectedObject,
        obj2: DetectedObject,
        positionTolerance: Float = 0.1f,
        timeTolerance: Long = 1000 // 1 segundo
    ): Boolean {
        // Verificar se são do mesmo tipo
        if (obj1.type != obj2.type) return false

        // Verificar se foram detectados muito próximos no tempo
        if (kotlin.math.abs(obj1.timestamp - obj2.timestamp) > timeTolerance) return false

        // Verificar posição se disponível
        val pos1 = obj1.position
        val pos2 = obj2.position

        if (pos1 != null && pos2 != null) {
            val distance = sqrt(
                (pos1.x - pos2.x).pow(2) +
                        (pos1.y - pos2.y).pow(2) +
                        (pos1.z - pos2.z).pow(2)
            )
            return distance <= positionTolerance
        }

        // Se não tem posição, verificar bounding box
        val bb1 = obj1.boundingBox
        val bb2 = obj2.boundingBox

        if (bb1 != null && bb2 != null) {
            val centerDistance = sqrt(
                (bb1.centerX - bb2.centerX).pow(2) +
                        (bb1.centerY - bb2.centerY).pow(2)
            )
            val avgSize = (bb1.width() + bb1.height() + bb2.width() + bb2.height()) / 4
            return centerDistance <= avgSize * 0.2f // 20% do tamanho médio
        }

        return false
    }

    companion object {
        private const val DEFAULT_MAX_CACHE_SIZE = 50
        private const val DEFAULT_CONFIDENCE_THRESHOLD = 0.7f
    }
}