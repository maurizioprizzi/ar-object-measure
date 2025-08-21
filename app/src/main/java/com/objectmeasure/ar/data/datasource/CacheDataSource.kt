package com.objectmeasure.ar.data.datasource

import com.objectmeasure.ar.core.util.*
import com.objectmeasure.ar.domain.model.DetectedObject
import com.objectmeasure.ar.domain.model.ObjectType
import com.objectmeasure.ar.domain.model.Position3D
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.*

/**
 * DataSource para cache em memória de objetos detectados - VERSÃO OTIMIZADA v2.2
 *
 * Melhorias implementadas:
 * - Performance otimizada com operações eficientes
 * - Thread safety completo
 * - Memory management aprimorado
 * - Algoritmos mais eficientes
 * - Validações robustas
 * - Cache inteligente com políticas configuráveis
 */
@Singleton
class CacheDataSource @Inject constructor() {

    private val mutex = Mutex()

    // Cache principal com capacidade inicial otimizada
    private val _cachedObjects = MutableStateFlow<List<DetectedObject>>(emptyList())
    val cachedObjects: Flow<List<DetectedObject>> = _cachedObjects.asStateFlow()

    // Configurações do cache
    private var maxCacheSize = DEFAULT_MAX_CACHE_SIZE
    private var autoCleanupEnabled = true
    private var confidenceThreshold = DEFAULT_CONFIDENCE_THRESHOLD
    private var maxObjectAgeMs = DEFAULT_MAX_OBJECT_AGE_MS
    private var duplicateDetectionEnabled = true

    // Cache de estatísticas para evitar recálculos
    private var cachedStatistics: CacheStatistics? = null
    private var statisticsLastUpdated = 0L

    // ========== OPERAÇÕES BÁSICAS ==========

    /**
     * Salva lista de objetos no cache (thread-safe e otimizada)
     */
    suspend fun cacheObjects(objects: List<DetectedObject>) = mutex.withLock {
        val validObjects = objects
            .filter { it.confidence.isHighConfidence(confidenceThreshold) }
            .distinctBy { it.id } // Remove duplicatas por ID

        _cachedObjects.value = if (validObjects.size > maxCacheSize) {
            // Manter os mais recentes e com maior confidence
            validObjects
                .sortedWith(
                    compareByDescending<DetectedObject> { it.confidence }
                        .thenByDescending { it.timestamp }
                )
                .take(maxCacheSize)
        } else {
            validObjects
        }

        invalidateStatisticsCache()
        validObjects.size.logDebug("Cached objects", "Cache")
    }

    /**
     * Adiciona um objeto ao cache existente (otimizada para performance)
     */
    suspend fun addObject(detectedObject: DetectedObject) = mutex.withLock {
        // Validações rápidas primeiro
        if (!detectedObject.confidence.isHighConfidence(confidenceThreshold)) {
            "Object filtered: low confidence ${detectedObject.confidence}".logDebug("Cache")
            return@withLock
        }

        val currentList = _cachedObjects.value

        // Verificar duplicatas se habilitado
        val existingIndex = if (duplicateDetectionEnabled) {
            currentList.indexOfFirst { existing ->
                existing.id == detectedObject.id || areSimilarObjects(existing, detectedObject)
            }
        } else {
            currentList.indexOfFirst { it.id == detectedObject.id }
        }

        val updatedList = if (existingIndex >= 0) {
            // Atualizar objeto existente de forma eficiente
            currentList.mapIndexed { index, obj ->
                if (index == existingIndex) detectedObject else obj
            }
        } else {
            // Adicionar novo objeto com política de tamanho
            if (currentList.size >= maxCacheSize) {
                // Remove o mais antigo e adiciona o novo
                currentList.drop(1) + detectedObject
            } else {
                currentList + detectedObject
            }
        }

        _cachedObjects.value = updatedList
        invalidateStatisticsCache()

        if (autoCleanupEnabled && shouldPerformCleanup()) {
            performAutoCleanupInternal()
        }
    }

    /**
     * Obtém objetos do cache (thread-safe)
     */
    suspend fun getCachedObjects(): List<DetectedObject> = mutex.withLock {
        _cachedObjects.value.toList()
    }

    /**
     * Versão não-bloqueante para leitura (snapshot atual)
     */
    fun getCachedObjectsSnapshot(): List<DetectedObject> {
        return _cachedObjects.value.toList()
    }

    /**
     * Limpa o cache completamente
     */
    suspend fun clearCache() = mutex.withLock {
        _cachedObjects.value = emptyList()
        invalidateStatisticsCache()
        "Cache cleared".logDebug("Cache")
    }

    // ========== OPERAÇÕES AVANÇADAS ==========

    /**
     * Remove objetos por ID (otimizada)
     */
    suspend fun removeObject(objectId: String) = mutex.withLock {
        val sizeBefore = _cachedObjects.value.size
        _cachedObjects.value = _cachedObjects.value.filterNot { it.id == objectId }

        if (_cachedObjects.value.size < sizeBefore) {
            invalidateStatisticsCache()
        }
    }

    /**
     * Remove múltiplos objetos por IDs (otimizada com Set lookup)
     */
    suspend fun removeObjects(objectIds: List<String>) = mutex.withLock {
        if (objectIds.isEmpty()) return@withLock

        val idsSet = objectIds.toSet()
        val sizeBefore = _cachedObjects.value.size

        _cachedObjects.value = _cachedObjects.value.filterNot { it.id in idsSet }

        if (_cachedObjects.value.size < sizeBefore) {
            invalidateStatisticsCache()
        }
    }

    /**
     * Atualiza objeto existente
     */
    suspend fun updateObject(updatedObject: DetectedObject) = mutex.withLock {
        var updated = false
        _cachedObjects.value = _cachedObjects.value.map { obj ->
            if (obj.id == updatedObject.id) {
                updated = true
                updatedObject
            } else {
                obj
            }
        }

        if (updated) {
            invalidateStatisticsCache()
        }
    }

    /**
     * Substitui objeto existente ou adiciona se não existir (upsert otimizado)
     */
    suspend fun upsertObject(detectedObject: DetectedObject) = mutex.withLock {
        val currentList = _cachedObjects.value
        val existingIndex = currentList.indexOfFirst { it.id == detectedObject.id }

        _cachedObjects.value = if (existingIndex >= 0) {
            currentList.mapIndexed { index, obj ->
                if (index == existingIndex) detectedObject else obj
            }
        } else {
            if (currentList.size >= maxCacheSize) {
                currentList.drop(1) + detectedObject
            } else {
                currentList + detectedObject
            }
        }

        invalidateStatisticsCache()
    }

    // ========== CONSULTAS ESPECÍFICAS PARA AR ==========

    /**
     * Busca objetos por proximidade 3D (otimizada para AR)
     */
    fun getObjectsNearPosition(
        x: Float,
        y: Float,
        z: Float,
        radius: Float
    ): Flow<List<DetectedObject>> {
        val radiusSquared = radius * radius // Evita sqrt na comparação

        return _cachedObjects.map { objects ->
            objects.filter { obj ->
                obj.position?.let { pos ->
                    val distanceSquared = (pos.x - x).pow(2) + (pos.y - y).pow(2) + (pos.z - z).pow(2)
                    distanceSquared <= radiusSquared
                } ?: false
            }
        }
    }

    /**
     * Filtra por tipo de objeto com cache
     */
    fun getObjectsByType(type: ObjectType): Flow<List<DetectedObject>> {
        return _cachedObjects.map { objects ->
            objects.filter { it.type == type }
        }
    }

    /**
     * Filtra por múltiplos tipos (otimizada com Set lookup)
     */
    fun getObjectsByTypes(types: List<ObjectType>): Flow<List<DetectedObject>> {
        val typesSet = types.toSet()
        return _cachedObjects.map { objects ->
            objects.filter { it.type in typesSet }
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
     * Obtém objetos detectados recentemente (otimizada)
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
        val objects = _cachedObjects.value
        return if (limit >= objects.size) {
            objects.toList()
        } else {
            objects.takeLast(limit)
        }
    }

    /**
     * Obtém histórico filtrado (otimizada com early returns)
     */
    fun getFilteredHistory(
        objectTypes: List<ObjectType>? = null,
        minConfidence: Float = 0.0f,
        since: Long? = null,
        limit: Int = 10
    ): List<DetectedObject> {
        val objects = _cachedObjects.value
        if (objects.isEmpty()) return emptyList()

        val typesSet = objectTypes?.toSet()

        return objects.asSequence()
            .let { sequence ->
                if (typesSet != null) {
                    sequence.filter { it.type in typesSet }
                } else sequence
            }
            .filter { it.confidence >= minConfidence }
            .let { sequence ->
                if (since != null) {
                    sequence.filter { it.timestamp >= since }
                } else sequence
            }
            .takeLast(limit)
            .toList()
    }

    // ========== ESTATÍSTICAS E ANÁLISES ==========

    /**
     * Obtém estatísticas do cache com cache interno
     */
    fun getCacheStatistics(): CacheStatistics {
        val currentTime = System.currentTimeMillis()

        // Usar cache de estatísticas se ainda válido (5 segundos)
        cachedStatistics?.let { stats ->
            if (currentTime - statisticsLastUpdated < 5000) {
                return stats
            }
        }

        val objects = _cachedObjects.value
        val statistics = if (objects.isEmpty()) {
            CacheStatistics.empty(maxCacheSize)
        } else {
            val confidences = objects.map { it.confidence }

            CacheStatistics(
                totalObjects = objects.size,
                reliableObjects = objects.count { it.isReliableDetection() },
                averageConfidence = confidences.average().toFloat(),
                maxConfidence = confidences.maxOrNull() ?: 0f,
                minConfidence = confidences.minOrNull() ?: 0f,
                typeDistribution = objects.groupingBy { it.type }.eachCount(),
                oldestTimestamp = objects.minOfOrNull { it.timestamp } ?: 0L,
                newestTimestamp = objects.maxOfOrNull { it.timestamp } ?: 0L,
                cacheUtilization = objects.size.toFloat() / maxCacheSize,
                memoryUsageEstimate = estimateMemoryUsage(objects)
            )
        }

        cachedStatistics = statistics
        statisticsLastUpdated = currentTime
        return statistics
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
     * Obtém distribuição de confidence (otimizada)
     */
    fun getConfidenceDistribution(): Map<String, Int> {
        val objects = _cachedObjects.value
        if (objects.isEmpty()) {
            return mapOf(
                "Very High (>0.9)" to 0,
                "High (0.8-0.9)" to 0,
                "Medium (0.7-0.8)" to 0,
                "Low (<0.7)" to 0
            )
        }

        var veryHigh = 0
        var high = 0
        var medium = 0
        var low = 0

        objects.forEach { obj ->
            when {
                obj.confidence > 0.9f -> veryHigh++
                obj.confidence >= 0.8f -> high++
                obj.confidence >= 0.7f -> medium++
                else -> low++
            }
        }

        return mapOf(
            "Very High (>0.9)" to veryHigh,
            "High (0.8-0.9)" to high,
            "Medium (0.7-0.8)" to medium,
            "Low (<0.7)" to low
        )
    }

    // ========== CONFIGURAÇÕES ==========

    /**
     * Define tamanho máximo do cache
     */
    suspend fun setMaxCacheSize(newSize: Int) = mutex.withLock {
        require(newSize > 0) { "Cache size must be positive" }
        require(newSize <= MAX_ALLOWED_CACHE_SIZE) { "Cache size too large: $newSize > $MAX_ALLOWED_CACHE_SIZE" }

        maxCacheSize = newSize

        // Ajustar cache atual se necessário
        val currentObjects = _cachedObjects.value
        if (currentObjects.size > newSize) {
            _cachedObjects.value = currentObjects
                .sortedWith(
                    compareByDescending<DetectedObject> { it.confidence }
                        .thenByDescending { it.timestamp }
                )
                .take(newSize)
            invalidateStatisticsCache()
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
     * Define idade máxima dos objetos
     */
    fun setMaxObjectAge(ageMs: Long) {
        require(ageMs > 0) { "Max object age must be positive" }
        maxObjectAgeMs = ageMs
    }

    /**
     * Habilita/desabilita detecção de duplicatas
     */
    fun setDuplicateDetectionEnabled(enabled: Boolean) {
        duplicateDetectionEnabled = enabled
    }

    /**
     * Obtém configurações atuais do cache
     */
    fun getCacheConfiguration(): CacheConfiguration {
        return CacheConfiguration(
            maxSize = maxCacheSize,
            autoCleanupEnabled = autoCleanupEnabled,
            confidenceThreshold = confidenceThreshold,
            currentSize = _cachedObjects.value.size,
            maxObjectAgeMs = maxObjectAgeMs,
            duplicateDetectionEnabled = duplicateDetectionEnabled
        )
    }

    // ========== OPERAÇÕES DE LIMPEZA ==========

    /**
     * Remove objetos antigos baseado em timestamp
     */
    suspend fun removeOldObjects(maxAgeMs: Long = maxObjectAgeMs) = mutex.withLock {
        val cutoffTime = System.currentTimeMillis() - maxAgeMs
        val sizeBefore = _cachedObjects.value.size

        _cachedObjects.value = _cachedObjects.value.filter {
            it.timestamp >= cutoffTime
        }

        val removedCount = sizeBefore - _cachedObjects.value.size
        if (removedCount > 0) {
            invalidateStatisticsCache()
            removedCount.logDebug("Removed old objects", "AutoCleanup")
        }
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
        if (removedCount > 0) {
            invalidateStatisticsCache()
            removedCount.logDebug("Removed low confidence objects", "ConfidenceFilter")
        }
    }

    /**
     * Remove outliers baseado em confidence (usando extensão otimizada)
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
        if (removedCount > 0) {
            invalidateStatisticsCache()
            removedCount.logDebug("Removed confidence outliers")
        }
    }

    /**
     * Remove objetos duplicados por similaridade (otimizada)
     */
    suspend fun removeDuplicates() = mutex.withLock {
        val objects = _cachedObjects.value
        if (objects.size <= 1) return@withLock

        val uniqueObjects = objects.removeDuplicates() // Usar extensão otimizada

        val removedCount = objects.size - uniqueObjects.size
        if (removedCount > 0) {
            _cachedObjects.value = uniqueObjects
            invalidateStatisticsCache()
            removedCount.logDebug("Removed duplicate objects")
        }
    }

    /**
     * Compacta cache removendo objetos menos relevantes
     */
    suspend fun compactCache(targetSize: Int = maxCacheSize / 2) = mutex.withLock {
        val objects = _cachedObjects.value
        if (objects.size <= targetSize) return@withLock

        // Algoritmo de compactação inteligente
        val sortedObjects = objects.sortedWith(
            compareByDescending<DetectedObject> { it.confidence }
                .thenByDescending { it.timestamp }
                .thenBy { it.type.ordinal } // Prioridade por tipo
        )

        val sizeBefore = objects.size
        _cachedObjects.value = sortedObjects.take(targetSize)

        val removedCount = sizeBefore - _cachedObjects.value.size
        invalidateStatisticsCache()
        removedCount.logDebug("Compacted cache")
    }

    // ========== OPERAÇÕES PRIVADAS ==========

    /**
     * Verifica se deve executar limpeza automática
     */
    private fun shouldPerformCleanup(): Boolean {
        val objects = _cachedObjects.value
        val currentTime = System.currentTimeMillis()

        return objects.size >= maxCacheSize * 0.8 || // 80% de capacidade
                objects.any { currentTime - it.timestamp > maxObjectAgeMs } // Tem objetos antigos
    }

    /**
     * Limpeza automática otimizada (chamada internamente)
     */
    private suspend fun performAutoCleanupInternal() {
        val currentTime = System.currentTimeMillis()
        val cutoffTime = currentTime - maxObjectAgeMs

        var objects = _cachedObjects.value

        // Remove objetos antigos
        objects = objects.filter { it.timestamp >= cutoffTime }

        // Remove objetos com confidence muito baixa
        objects = objects.filter { it.confidence >= 0.3f }

        // Se ainda está muito cheio, compactar
        if (objects.size >= maxCacheSize) {
            objects = objects
                .sortedWith(
                    compareByDescending<DetectedObject> { it.confidence }
                        .thenByDescending { it.timestamp }
                )
                .take(maxCacheSize * 3 / 4) // Manter 75% da capacidade
        }

        if (objects.size != _cachedObjects.value.size) {
            _cachedObjects.value = objects
            invalidateStatisticsCache()
        }
    }

    /**
     * Verifica se dois objetos são similares (otimizada para evitar duplicatas)
     */
    private fun areSimilarObjects(
        obj1: DetectedObject,
        obj2: DetectedObject,
        positionTolerance: Float = 0.1f,
        timeTolerance: Long = 1000 // 1 segundo
    ): Boolean {
        // Verificações rápidas primeiro
        if (obj1.type != obj2.type) return false
        if (abs(obj1.timestamp - obj2.timestamp) > timeTolerance) return false

        // Verificar posição se disponível
        val pos1 = obj1.position
        val pos2 = obj2.position

        if (pos1 != null && pos2 != null) {
            val distanceSquared = (pos1.x - pos2.x).pow(2) +
                    (pos1.y - pos2.y).pow(2) +
                    (pos1.z - pos2.z).pow(2)
            return distanceSquared <= positionTolerance * positionTolerance
        }

        // Verificar bounding box usando extensões corretas
        val bb1 = obj1.boundingBox
        val bb2 = obj2.boundingBox

        if (bb1 != null && bb2 != null) {
            val centerDistance = sqrt(
                (bb1.safeCenterX - bb2.safeCenterX).pow(2) +
                        (bb1.safeCenterY - bb2.safeCenterY).pow(2)
            )
            val avgSize = (bb1.width() + bb1.height() + bb2.width() + bb2.height()) / 4
            return centerDistance <= avgSize * 0.2f // 20% do tamanho médio
        }

        return false
    }

    /**
     * Invalida cache de estatísticas
     */
    private fun invalidateStatisticsCache() {
        cachedStatistics = null
    }

    /**
     * Estima uso de memória dos objetos (aproximado)
     */
    private fun estimateMemoryUsage(objects: List<DetectedObject>): Long {
        // Estimativa básica: ~200 bytes por objeto + overhead
        return objects.size * 200L + objects.size * 64L // object overhead
    }

    companion object {
        private const val DEFAULT_MAX_CACHE_SIZE = 50
        private const val DEFAULT_CONFIDENCE_THRESHOLD = 0.7f
        private const val DEFAULT_MAX_OBJECT_AGE_MS = 5 * 60 * 1000L // 5 minutos
        private const val MAX_ALLOWED_CACHE_SIZE = 500
    }
}

// ========== DATA CLASSES ==========

/**
 * Estatísticas do cache
 */
data class CacheStatistics(
    val totalObjects: Int,
    val reliableObjects: Int,
    val averageConfidence: Float,
    val maxConfidence: Float,
    val minConfidence: Float,
    val typeDistribution: Map<ObjectType, Int>,
    val oldestTimestamp: Long,
    val newestTimestamp: Long,
    val cacheUtilization: Float,
    val memoryUsageEstimate: Long
) {
    companion object {
        fun empty(maxCacheSize: Int) = CacheStatistics(
            totalObjects = 0,
            reliableObjects = 0,
            averageConfidence = 0f,
            maxConfidence = 0f,
            minConfidence = 0f,
            typeDistribution = emptyMap(),
            oldestTimestamp = 0L,
            newestTimestamp = 0L,
            cacheUtilization = 0f,
            memoryUsageEstimate = 0L
        )
    }
}

/**
 * Configuração do cache
 */
data class CacheConfiguration(
    val maxSize: Int,
    val autoCleanupEnabled: Boolean,
    val confidenceThreshold: Float,
    val currentSize: Int,
    val maxObjectAgeMs: Long,
    val duplicateDetectionEnabled: Boolean
)

// ========== EXTENSIONS ==========

/**
 * Extensão para verificar se um objeto detectado é confiável
 */
fun DetectedObject.isReliableDetection(
    minConfidence: Float = 0.8f,
    maxAge: Long = 30_000L
): Boolean {
    val isRecentEnough = System.currentTimeMillis() - timestamp <= maxAge
    val hasGoodConfidence = confidence >= minConfidence
    val hasValidBoundingBox = boundingBox?.isValid() ?: false

    return isRecentEnough && hasGoodConfidence && hasValidBoundingBox
}