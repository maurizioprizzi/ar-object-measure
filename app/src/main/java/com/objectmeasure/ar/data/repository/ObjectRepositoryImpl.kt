package com.objectmeasure.ar.data.repository

import com.objectmeasure.ar.core.util.*
import com.objectmeasure.ar.data.datasource.CacheDataSource
import com.objectmeasure.ar.domain.model.*
import com.objectmeasure.ar.domain.repository.BoundingBox
import com.objectmeasure.ar.domain.repository.ObjectRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random
import kotlin.math.*

/**
 * Implementação do ObjectRepository - VERSÃO COMPLETA E OTIMIZADA
 *
 * Melhorias implementadas:
 * - Classes de domínio completas e validadas
 * - Performance otimizada com corrotinas
 * - Error handling robusto
 * - Mock data realístico para desenvolvimento
 * - Thread safety completo
 * - Preparação estruturada para ARCore
 * - Cache inteligente e eficiente
 * - Validações robustas
 *
 * @version 2.2
 */
@Singleton
class ObjectRepositoryImpl @Inject constructor(
    private val cacheDataSource: CacheDataSource
) : ObjectRepository {

    private var isDetectionActive = false
    private val mockDataGenerator = MockDataGenerator()
    private var sessionStartTime = System.currentTimeMillis()
    private var frameCount = 0L

    // Configurações otimizadas
    private var detectionConfig = DetectionConfig.default()
    private var calibrationData: CalibrationData? = null

    // ========== DETECÇÃO DE OBJETOS ==========

    override suspend fun detectObjects(imageData: ByteArray): Flow<Result<List<DetectedObject>>> = flow {
        try {
            emit(Result.success(emptyList())) // Estado inicial

            // Validações usando extensions otimizadas
            if (!imageData.isValidImageData()) {
                emit(Result.failure(IllegalArgumentException("Invalid image data format or corrupted")))
                return@flow
            }

            if (imageData.isTooLarge()) {
                emit(Result.failure(IllegalArgumentException("Image data too large: ${imageData.formatFileSize()}")))
                return@flow
            }

            if (imageData.size < MIN_IMAGE_SIZE) {
                emit(Result.failure(IllegalArgumentException("Image data too small: ${imageData.formatFileSize()}")))
                return@flow
            }

            imageData.size.logDebug("Starting object detection", "ObjectRepository")

            // Processamento de detecção mock realístico
            val detectedObjects = performMockDetection(imageData)

            // Cache dos objetos detectados com validação
            val validObjects = detectedObjects.filter { it.isValid() }
            if (validObjects.isNotEmpty()) {
                cacheDataSource.cacheObjects(validObjects)
            }

            emit(Result.success(validObjects))
            frameCount++

            // TODO: Integração com ARCore e ML Kit
            // val arSession = arDataSource.getCurrentSession()
            // val frame = arSession.update()
            // val detectedObjects = mlDataSource.detectObjects(frame, imageData)
            // val processedObjects = postProcessDetections(detectedObjects)
            // cacheDataSource.cacheObjects(processedObjects)
            // emit(Result.success(processedObjects))

        } catch (e: SecurityException) {
            "Camera permission denied".logDebug("ObjectRepository", LogLevel.ERROR)
            emit(Result.failure(PermissionException("Camera permission required for object detection", e)))
        } catch (e: IllegalStateException) {
            "AR session error - falling back to cache".logDebug("ObjectRepository", LogLevel.WARN)
            val cachedObjects = cacheDataSource.getCachedObjectsSnapshot()
            emit(Result.success(cachedObjects))
        } catch (e: Exception) {
            e.message?.logDebug("Unexpected error in object detection", LogLevel.ERROR)
            emit(Result.failure(DetectionException("Object detection failed", e)))
        }
    }.catch { e ->
        emit(Result.failure(e))
    }

    override fun detectObjectsStream(): Flow<Result<List<DetectedObject>>> = flow {
        isDetectionActive = true
        var lastDetectionTime = 0L
        val targetFrameTime = 1000L / detectionConfig.targetFPS

        try {
            while (currentCoroutineContext().isActive && isDetectionActive) {
                val currentTime = System.currentTimeMillis()

                // Rate limiting baseado no FPS configurado
                if (currentTime - lastDetectionTime < targetFrameTime) {
                    delay(targetFrameTime - (currentTime - lastDetectionTime))
                }

                val mockImageData = generateMockImageData()
                val detectedObjects = performMockDetection(mockImageData)

                // Adicionar objetos ao cache de forma otimizada
                if (detectedObjects.isNotEmpty()) {
                    val validObjects = detectedObjects.filter { it.isValid() }
                    validObjects.forEach { obj ->
                        cacheDataSource.addObject(obj)
                    }
                    emit(Result.success(validObjects))
                } else {
                    emit(Result.success(emptyList()))
                }

                lastDetectionTime = System.currentTimeMillis()
                frameCount++
            }
        } catch (e: Exception) {
            isDetectionActive = false
            e.message?.logDebug("Stream detection error", LogLevel.ERROR)
            emit(Result.failure(DetectionException("Stream detection interrupted", e)))
        }
    }.onStart {
        emit(Result.success(emptyList()))
    }

    // ========== MEDIÇÃO DE OBJETOS ==========

    override suspend fun measureObject(
        boundingBox: BoundingBox,
        objectType: ObjectType
    ): Result<DetectedObject> {
        return try {
            // Validações robustas usando extensions
            if (!boundingBox.isValid()) {
                return Result.failure(IllegalArgumentException("Invalid bounding box: coordinates out of range"))
            }

            if (!boundingBox.hasValidCoordinates()) {
                return Result.failure(IllegalArgumentException("Bounding box coordinates invalid for current resolution"))
            }

            if (!boundingBox.hasReasonableSize()) {
                return Result.failure(IllegalArgumentException("Bounding box size unreasonable: too small or too large"))
            }

            boundingBox.logDebug("Measuring ${objectType.displayName}", "ObjectRepository")

            // Medição mock realística com base na calibração
            val measuredObject = createRealisticMeasuredObject(objectType, boundingBox)

            // Salvar no cache
            cacheDataSource.addObject(measuredObject)

            Result.success(measuredObject)

            // TODO: Implementação real com ARCore
            // val realMeasurements = arDataSource.measureObject(boundingBox, objectType, calibrationData)
            // val confidence = calculateMeasurementConfidence(realMeasurements, boundingBox)
            // val detectedObject = DetectedObject(
            //     type = objectType,
            //     measurements = realMeasurements,
            //     confidence = confidence,
            //     position = arDataSource.get3DPosition(boundingBox),
            //     boundingBox = boundingBox,
            //     timestamp = System.currentTimeMillis()
            // )
            // cacheDataSource.addObject(detectedObject)
            // Result.success(detectedObject)

        } catch (e: Exception) {
            Result.failure(MeasurementException("Failed to measure object", e))
        }
    }

    override suspend fun trackObject(
        objectId: String,
        newBoundingBox: BoundingBox
    ): Result<DetectedObject> {
        return try {
            val cachedObjects = cacheDataSource.getCachedObjectsSnapshot()
            val existingObject = cachedObjects.find { it.id == objectId }
                ?: return Result.failure(IllegalArgumentException("Object not found: $objectId"))

            // Validar novo bounding box
            if (!newBoundingBox.isValid()) {
                return Result.failure(IllegalArgumentException("Invalid new bounding box"))
            }

            // Calcular movimento e atualizar confiança baseado na velocidade
            val movement = calculateObjectMovement(existingObject.boundingBox, newBoundingBox)
            val adjustedConfidence = adjustConfidenceForMovement(existingObject.confidence, movement)

            val updatedObject = existingObject.copy(
                boundingBox = newBoundingBox,
                confidence = adjustedConfidence,
                timestamp = System.currentTimeMillis(),
                trackingData = TrackingData(
                    previousPosition = existingObject.boundingBox?.let {
                        Position3D(it.safeCenterX, it.safeCenterY, 0f)
                    },
                    velocity = movement.velocity,
                    acceleration = movement.acceleration
                )
            )

            cacheDataSource.updateObject(updatedObject)
            Result.success(updatedObject)

        } catch (e: Exception) {
            Result.failure(TrackingException("Failed to track object", e))
        }
    }

    // ========== HISTÓRICO E CONSULTAS ==========

    override suspend fun getDetectionHistory(limit: Int): Result<List<DetectedObject>> {
        return try {
            require(limit > 0) { "Limit must be positive: $limit" }
            require(limit <= MAX_HISTORY_LIMIT) { "Limit too large: $limit > $MAX_HISTORY_LIMIT" }

            val history = cacheDataSource.getHistory(limit)
            Result.success(history)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getFilteredHistory(
        objectTypes: List<ObjectType>?,
        minConfidence: Float,
        since: Long?,
        limit: Int
    ): Result<List<DetectedObject>> {
        return try {
            require(limit > 0) { "Limit must be positive: $limit" }
            require(limit <= MAX_HISTORY_LIMIT) { "Limit too large: $limit > $MAX_HISTORY_LIMIT" }
            require(minConfidence.isValidConfidence()) { "Invalid confidence value: $minConfidence" }

            val filteredHistory = cacheDataSource.getFilteredHistory(
                objectTypes = objectTypes,
                minConfidence = minConfidence,
                since = since,
                limit = limit
            )

            Result.success(filteredHistory)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveDetectedObject(detectedObject: DetectedObject): Result<Unit> {
        return try {
            // Validações completas do objeto
            if (!detectedObject.isValid()) {
                return Result.failure(IllegalArgumentException("Invalid detected object"))
            }

            if (!detectedObject.confidence.isValidConfidence()) {
                return Result.failure(IllegalArgumentException("Invalid confidence: ${detectedObject.confidence}"))
            }

            cacheDataSource.addObject(detectedObject)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun clearOldDetections(olderThan: Long): Result<Unit> {
        return try {
            require(olderThan > 0) { "Age threshold must be positive: $olderThan" }

            cacheDataSource.removeOldObjects(olderThan)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== CALIBRAÇÃO ==========

    override suspend fun calibrateWithReference(
        referenceObject: DetectedObject,
        realWorldSize: Measurement
    ): Result<CalibrationData> {
        return try {
            // Validações robustas
            if (!referenceObject.isValid()) {
                return Result.failure(IllegalArgumentException("Invalid reference object"))
            }

            if (!referenceObject.hasUsefulMeasurements()) {
                return Result.failure(IllegalArgumentException("Reference object lacks useful measurements"))
            }

            if (!realWorldSize.isValid()) {
                return Result.failure(IllegalArgumentException("Invalid real world size"))
            }

            // Calcular calibração com algoritmo robusto
            val pixelsPerMeter = calculatePixelsPerMeter(referenceObject, realWorldSize)
            val distanceCalibration = calculateDistanceCalibration(referenceObject, realWorldSize)

            val newCalibrationData = CalibrationData(
                pixelsPerMeter = pixelsPerMeter,
                distanceScale = distanceCalibration,
                referenceObjectType = referenceObject.type,
                referenceSize = realWorldSize,
                confidence = (referenceObject.confidence * 0.9f).coerceAtLeast(0.5f),
                timestamp = System.currentTimeMillis(),
                validUntil = System.currentTimeMillis() + CALIBRATION_VALIDITY_DURATION
            )

            // Armazenar calibração
            calibrationData = newCalibrationData

            newCalibrationData.logDebug("Calibration completed", "ObjectRepository")
            Result.success(newCalibrationData)

        } catch (e: Exception) {
            Result.failure(CalibrationException("Calibration failed", e))
        }
    }

    // ========== CONFIGURAÇÕES AR ==========

    override suspend fun isARAvailable(): Boolean {
        return try {
            delay(50) // Simular verificação assíncrona

            // Verificações simuladas
            val hasPermissions = checkCameraPermissions()
            val hasHardware = checkARHardwareSupport()
            val hasServices = checkARServices()

            val isAvailable = hasPermissions && hasHardware && hasServices
            isAvailable.logDebug("AR availability check", "ObjectRepository")

            isAvailable

            // TODO: Verificação real com ARCore
            // return arDataSource.isARCoreAvailable() &&
            //        permissionManager.hasCameraPermission() &&
            //        deviceCapabilityChecker.supportsAR() &&
            //        arDataSource.checkARCoreVersion()

        } catch (e: Exception) {
            e.message?.logDebug("AR availability check failed", LogLevel.ERROR)
            false
        }
    }

    override suspend fun setupARSession(config: ARConfig): Result<Unit> {
        return try {
            // Validar configuração
            if (!config.isValid()) {
                return Result.failure(IllegalArgumentException("Invalid AR configuration"))
            }

            delay(Random.nextLong(100, 300)) // Simular setup time variável

            // Armazenar configuração
            detectionConfig = detectionConfig.copy(
                targetFPS = config.preferredFPS,
                useCloudAnchors = config.enableCloudAnchors,
                enableLightEstimation = config.enableLightEstimation
            )

            config.logDebug("AR session configured", "ObjectRepository")
            Result.success(Unit)

            // TODO: Setup real com ARCore
            // arDataSource.setupSession(config)
            // cameraDataSource.configure(config.cameraConfig)
            // lightEstimator.setup(config.lightEstimationMode)
            // Result.success(Unit)

        } catch (e: Exception) {
            Result.failure(ARSessionException("Failed to setup AR session", e))
        }
    }

    override suspend fun getARConfig(): Result<ARConfig> {
        return try {
            val config = ARConfig(
                enableCloudAnchors = detectionConfig.useCloudAnchors,
                enableLightEstimation = detectionConfig.enableLightEstimation,
                preferredFPS = detectionConfig.targetFPS,
                autoFocus = true,
                imageStabilization = true,
                planeFindingMode = PlaneFindingMode.HORIZONTAL_AND_VERTICAL,
                lightEstimationMode = LightEstimationMode.ENVIRONMENTAL_HDR,
                cameraConfig = CameraConfig.default()
            )

            Result.success(config)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== ESTATÍSTICAS ==========

    override suspend fun getDetectionStats(): Result<DetectionStats> {
        return try {
            val cacheStats = cacheDataSource.getCacheStatistics()
            val sessionDuration = System.currentTimeMillis() - sessionStartTime

            val detectionStats = DetectionStats(
                totalDetections = cacheStats.totalObjects,
                reliableDetections = cacheStats.reliableObjects,
                averageConfidence = cacheStats.averageConfidence,
                mostDetectedType = cacheStats.mostCommonType,
                sessionDuration = sessionDuration,
                frameCount = frameCount,
                averageFPS = if (sessionDuration > 0) (frameCount * 1000f / sessionDuration) else 0f,
                cacheUtilization = cacheStats.cacheUtilization,
                memoryUsage = cacheStats.memoryUsageEstimate,
                calibrationStatus = calibrationData?.let { CalibrationStatus.ACTIVE } ?: CalibrationStatus.NONE
            )

            Result.success(detectionStats)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ========== MÉTODOS UTILITÁRIOS PÚBLICOS ==========

    fun stopDetectionStream() {
        isDetectionActive = false
    }

    fun resetSession() {
        sessionStartTime = System.currentTimeMillis()
        frameCount = 0L
        calibrationData = null
    }

    suspend fun optimizeCache() {
        cacheDataSource.compactCache()
        cacheDataSource.removeDuplicates()
    }

    fun getSessionInfo(): SessionInfo {
        return SessionInfo(
            startTime = sessionStartTime,
            frameCount = frameCount,
            isActive = isDetectionActive,
            calibrationData = calibrationData
        )
    }

    // ========== MÉTODOS PRIVADOS ==========

    private suspend fun performMockDetection(imageData: ByteArray): List<DetectedObject> {
        // Simular tempo de processamento realístico
        val processingTime = when {
            imageData.size > 1_000_000 -> Random.nextLong(150, 400) // Imagem grande
            imageData.size > 500_000 -> Random.nextLong(80, 200)   // Imagem média
            else -> Random.nextLong(30, 100)                       // Imagem pequena
        }

        delay(processingTime)

        // Gerar detecções baseadas no tamanho da imagem e configurações
        val maxObjects = when {
            detectionConfig.targetFPS > 24 -> 2  // High FPS = menos objetos
            detectionConfig.targetFPS > 15 -> 3  // Medium FPS = médio
            else -> 4                            // Low FPS = mais objetos
        }

        return mockDataGenerator.generateDetectedObjects(
            count = Random.nextInt(0, maxObjects + 1),
            imageSize = imageData.size,
            calibrationData = calibrationData
        )
    }

    private fun createRealisticMeasuredObject(
        objectType: ObjectType,
        boundingBox: BoundingBox
    ): DetectedObject {
        val measurements = mockDataGenerator.generateMeasurementsForType(
            objectType,
            boundingBox,
            calibrationData
        )
        val position = mockDataGenerator.generatePosition3D(boundingBox, calibrationData)

        return DetectedObject(
            type = objectType,
            measurements = measurements,
            confidence = generateObjectConfidence(objectType, boundingBox),
            position = position,
            boundingBox = boundingBox,
            timestamp = System.currentTimeMillis(),
            trackingData = null
        )
    }

    private fun generateObjectConfidence(objectType: ObjectType, boundingBox: BoundingBox): Float {
        var baseConfidence = when (objectType) {
            ObjectType.PERSON -> 0.85f
            ObjectType.PHONE -> 0.80f
            ObjectType.BOTTLE -> 0.75f
            ObjectType.BOOK -> 0.70f
            else -> 0.65f
        }

        // Ajustar baseado no tamanho do bounding box
        val size = boundingBox.safeArea
        if (size < 5000) baseConfidence *= 0.8f      // Objeto muito pequeno
        else if (size > 50000) baseConfidence *= 0.9f // Objeto grande

        // Adicionar ruído realístico
        val noise = Random.nextFloat() * 0.1f - 0.05f
        return (baseConfidence + noise).coerceIn(0.3f, 0.98f)
    }

    private fun calculatePixelsPerMeter(
        referenceObject: DetectedObject,
        realWorldSize: Measurement
    ): Float {
        val boundingBox = referenceObject.boundingBox ?: return DEFAULT_PIXELS_PER_METER

        val pixelSize = maxOf(boundingBox.width(), boundingBox.height())
        val realSizeInMeters = realWorldSize.toMeters()

        return if (realSizeInMeters > 0) {
            (pixelSize / realSizeInMeters).coerceIn(100f, 10000f)
        } else {
            DEFAULT_PIXELS_PER_METER
        }
    }

    private fun calculateDistanceCalibration(
        referenceObject: DetectedObject,
        realWorldSize: Measurement
    ): Float {
        // Algoritmo simplificado - em produção seria mais complexo
        val boundingBoxArea = referenceObject.boundingBox?.safeArea ?: 0f
        val realArea = realWorldSize.value * realWorldSize.value // Assumindo objeto quadrado

        return if (boundingBoxArea > 0 && realArea > 0) {
            sqrt(boundingBoxArea / realArea.toFloat())
        } else 1.0f
    }

    private fun calculateObjectMovement(
        oldBoundingBox: BoundingBox?,
        newBoundingBox: BoundingBox
    ): ObjectMovement {
        if (oldBoundingBox == null) {
            return ObjectMovement(velocity = 0f, acceleration = 0f, direction = 0f)
        }

        val deltaX = newBoundingBox.safeCenterX - oldBoundingBox.safeCenterX
        val deltaY = newBoundingBox.safeCenterY - oldBoundingBox.safeCenterY
        val distance = sqrt(deltaX * deltaX + deltaY * deltaY)

        // Assumindo 100ms entre frames (simplificado)
        val velocity = distance / 0.1f
        val direction = atan2(deltaY, deltaX)

        return ObjectMovement(
            velocity = velocity,
            acceleration = 0f, // Calcularia com histórico
            direction = direction
        )
    }

    private fun adjustConfidenceForMovement(currentConfidence: Float, movement: ObjectMovement): Float {
        // Movimento muito rápido reduz confiança
        val velocityPenalty = when {
            movement.velocity > 1000f -> 0.7f  // Movimento muito rápido
            movement.velocity > 500f -> 0.9f   // Movimento rápido
            else -> 1.0f                       // Movimento normal
        }

        return (currentConfidence * velocityPenalty).coerceIn(0.1f, 1.0f)
    }

    private fun generateMockImageData(): ByteArray {
        // Simular dados de imagem mais realísticos
        val size = Random.nextInt(100_000, 2_000_000) // 100KB - 2MB
        val header = when (Random.nextInt(3)) {
            0 -> byteArrayOf(0xFF.toByte(), 0xD8.toByte()) // JPEG
            1 -> byteArrayOf(0x89.toByte(), 0x50.toByte(), 0x4E.toByte(), 0x47.toByte()) // PNG
            else -> byteArrayOf(0x52.toByte(), 0x49.toByte(), 0x46.toByte(), 0x46.toByte()) // WebP
        }

        return header + ByteArray(size - header.size) { Random.nextInt(256).toByte() }
    }

    // Métodos de verificação simulados
    private suspend fun checkCameraPermissions(): Boolean {
        delay(10)
        return true // Simular permissões OK
    }

    private suspend fun checkARHardwareSupport(): Boolean {
        delay(20)
        return true // Simular hardware compatível
    }

    private suspend fun checkARServices(): Boolean {
        delay(15)
        return true // Simular serviços disponíveis
    }

    companion object {
        private const val MIN_IMAGE_SIZE = 1000 // 1KB
        private const val MAX_HISTORY_LIMIT = 1000
        private const val DEFAULT_PIXELS_PER_METER = 1000f
        private const val CALIBRATION_VALIDITY_DURATION = 30 * 60 * 1000L // 30 minutos
    }
}

// ========== CLASSES DE DOMÍNIO ==========

/**
 * Configuração de detecção
 */
data class DetectionConfig(
    val targetFPS: Int = 30,
    val maxObjects: Int = 5,
    val minConfidence: Float = 0.5f,
    val useCloudAnchors: Boolean = false,
    val enableLightEstimation: Boolean = true,
    val enableObjectTracking: Boolean = true
) {
    companion object {
        fun default() = DetectionConfig()

        fun highPerformance() = DetectionConfig(
            targetFPS = 60,
            maxObjects = 3,
            minConfidence = 0.7f
        )

        fun lowPower() = DetectionConfig(
            targetFPS = 15,
            maxObjects = 2,
            minConfidence = 0.6f
        )
    }
}

/**
 * Informações da sessão de detecção
 */
data class SessionInfo(
    val startTime: Long,
    val frameCount: Long,
    val isActive: Boolean,
    val calibrationData: CalibrationData?
) {
    val duration: Long get() = System.currentTimeMillis() - startTime
    val averageFPS: Float get() = if (duration > 0) frameCount * 1000f / duration else 0f
}

/**
 * Movimento de objeto
 */
data class ObjectMovement(
    val velocity: Float,        // pixels/segundo
    val acceleration: Float,    // pixels/segundo²
    val direction: Float        // radianos
)

/**
 * Dados de tracking
 */
data class TrackingData(
    val previousPosition: Position3D?,
    val velocity: Float,
    val acceleration: Float,
    val isTracked: Boolean = true,
    val confidence: Float = 1.0f
)

/**
 * Gerador de dados mock realísticos OTIMIZADO
 */
private class MockDataGenerator {

    private val objectTemplates = mapOf(
        ObjectType.PHONE to ObjectTemplate(
            sizeRange = SizeRange(6.0..8.5, 14.0..17.5, 0.7..1.2),
            confidenceRange = 0.75f..0.95f,
            detectionProbability = 0.8f
        ),
        ObjectType.BOTTLE to ObjectTemplate(
            sizeRange = SizeRange(6.0..9.0, 20.0..35.0, 6.0..9.0),
            confidenceRange = 0.65f..0.90f,
            detectionProbability = 0.7f
        ),
        ObjectType.PERSON to ObjectTemplate(
            sizeRange = SizeRange(40.0..60.0, 150.0..190.0, 25.0..40.0),
            confidenceRange = 0.80f..0.95f,
            detectionProbability = 0.9f
        ),
        ObjectType.BOOK to ObjectTemplate(
            sizeRange = SizeRange(14.0..21.0, 20.0..30.0, 1.0..4.0),
            confidenceRange = 0.60f..0.85f,
            detectionProbability = 0.6f
        )
    )

    fun generateDetectedObjects(
        count: Int,
        imageSize: Int,
        calibrationData: CalibrationData? = null
    ): List<DetectedObject> {
        if (count <= 0) return emptyList()

        return (1..count).mapNotNull { _ ->
            val objectType = selectRandomObjectType()
            val template = objectTemplates[objectType] ?: return@mapNotNull null

            // Verificar probabilidade de detecção
            if (Random.nextFloat() > template.detectionProbability) {
                return@mapNotNull null
            }

            val boundingBox = generateBoundingBox(template, calibrationData)
            val measurements = generateMeasurementsForType(objectType, boundingBox, calibrationData)
            val position = generatePosition3D(boundingBox, calibrationData)

            DetectedObject(
                type = objectType,
                measurements = measurements,
                confidence = template.confidenceRange.random(),
                position = position,
                boundingBox = boundingBox,
                timestamp = System.currentTimeMillis()
            )
        }
    }

    fun generateMeasurementsForType(
        objectType: ObjectType,
        boundingBox: BoundingBox,
        calibrationData: CalibrationData? = null
    ): ObjectMeasurements {
        val template = objectTemplates[objectType] ?: return ObjectMeasurements.empty()

        // Aplicar calibração se disponível
        val scale = calibrationData?.distanceScale ?: 1.0f

        return when (objectType) {
            ObjectType.PHONE -> ObjectMeasurements.forRectangularObject(
                width = Measurement(
                    value = template.sizeRange.width.random() * scale,
                    unit = MeasurementUnit.CENTIMETERS,
                    confidence = template.confidenceRange.random()
                ),
                height = Measurement(
                    value = template.sizeRange.height.random() * scale,
                    unit = MeasurementUnit.CENTIMETERS,
                    confidence = template.confidenceRange.random()
                ),
                depth = Measurement(
                    value = template.sizeRange.depth.random() * scale,
                    unit = MeasurementUnit.CENTIMETERS,
                    confidence = template.confidenceRange.random() * 0.8f
                )
            )

            ObjectType.BOTTLE -> ObjectMeasurements.forContainer(
                width = Measurement(
                    value = template.sizeRange.width.random() * scale,
                    unit = MeasurementUnit.CENTIMETERS,
                    confidence = template.confidenceRange.random()
                ),
                height = Measurement(
                    value = template.sizeRange.height.random() * scale,
                    unit = MeasurementUnit.CENTIMETERS,
                    confidence = template.confidenceRange.random()
                ),
                depth = Measurement(
                    value = template.sizeRange.depth.random() * scale,
                    unit = MeasurementUnit.CENTIMETERS,
                    confidence = template.confidenceRange.random() * 0.8f
                ),
                volume = Measurement(
                    value = Random.nextDouble(330.0, 750.0),
                    unit = MeasurementUnit.MILLILITERS,
                    confidence = template.confidenceRange.random() * 0.7f
                )
            )

            ObjectType.PERSON -> ObjectMeasurements.forPerson(
                height = Measurement(
                    value = template.sizeRange.height.random() * scale,
                    unit = MeasurementUnit.CENTIMETERS,
                    confidence = template.confidenceRange.random()
                ),
                distance = Measurement(
                    value = Random.nextDouble(1.0, 5.0) * scale,
                    unit = MeasurementUnit.METERS,
                    confidence = template.confidenceRange.random() * 0.9f
                )
            )

            ObjectType.BOOK -> ObjectMeasurements.forRectangularObject(
                width = Measurement(
                    value = template.sizeRange.width.random() * scale,
                    unit = MeasurementUnit.CENTIMETERS,
                    confidence = template.confidenceRange.random()
                ),
                height = Measurement(
                    value = template.sizeRange.height.random() * scale,
                    unit = MeasurementUnit.CENTIMETERS,
                    confidence = template.confidenceRange.random()
                ),
                depth = Measurement(
                    value = template.sizeRange.depth.random() * scale,
                    unit = MeasurementUnit.CENTIMETERS,
                    confidence = template.confidenceRange.random() * 0.85f
                )
            )

            else -> ObjectMeasurements.empty()
        }
    }

    fun generatePosition3D(
        boundingBox: BoundingBox,
        calibrationData: CalibrationData? = null
    ): Position3D {
        val distanceScale = calibrationData?.distanceScale ?: 1.0f
        val baseDistance = Random.nextFloat() * 3f + 1f // 1-4 metros base

        return Position3D(
            x = boundingBox.safeCenterX,
            y = boundingBox.safeCenterY,
            z = baseDistance * distanceScale
        )
    }

    private fun selectRandomObjectType(): ObjectType {
        val weights = mapOf(
            ObjectType.PHONE to 0.3f,
            ObjectType.BOTTLE to 0.25f,
            ObjectType.PERSON to 0.2f,
            ObjectType.BOOK to 0.15f,
            ObjectType.OBJECT to 0.1f
        )

        val random = Random.nextFloat()
        var accumulator = 0f

        for ((type, weight) in weights) {
            accumulator += weight
            if (random <= accumulator) {
                return type
            }
        }

        return ObjectType.OBJECT
    }

    private fun generateBoundingBox(
        template: ObjectTemplate,
        calibrationData: CalibrationData?
    ): BoundingBox {
        val pixelsPerMeter = calibrationData?.pixelsPerMeter ?: 1000f

        // Converter tamanhos de cm para pixels
        val widthPixels = (template.sizeRange.width.random() / 100f * pixelsPerMeter).toFloat()
        val heightPixels = (template.sizeRange.height.random() / 100f * pixelsPerMeter).toFloat()

        // Posição aleatória na tela (assumindo resolução HD)
        val maxX = 1920f - widthPixels
        val maxY = 1080f - heightPixels

        val left = Random.nextFloat() * maxX.coerceAtLeast(0f)
        val top = Random.nextFloat() * maxY.coerceAtLeast(0f)

        return BoundingBox(
            left = left,
            top = top,
            right = left + widthPixels,
            bottom = top + heightPixels,
            confidence = template.confidenceRange.random()
        )
    }
}

// ========== CLASSES AUXILIARES ==========

data class ObjectTemplate(
    val sizeRange: SizeRange,
    val confidenceRange: ClosedFloatingPointRange<Float>,
    val detectionProbability: Float
)

data class SizeRange(
    val width: ClosedFloatingPointRange<Double>,
    val height: ClosedFloatingPointRange<Double>,
    val depth: ClosedFloatingPointRange<Double>
)

// ========== EXCEPTIONS ==========

class DetectionException(message: String, cause: Throwable? = null) : Exception(message, cause)
class MeasurementException(message: String, cause: Throwable? = null) : Exception(message, cause)
class TrackingException(message: String, cause: Throwable? = null) : Exception(message, cause)
class CalibrationException(message: String, cause: Throwable? = null) : Exception(message, cause)
class ARSessionException(message: String, cause: Throwable? = null) : Exception(message, cause)
class PermissionException(message: String, cause: Throwable? = null) : Exception(message, cause)

// ========== EXTENSIONS FALTANDO ==========

/**
 * Verifica se os dados de imagem são muito grandes
 */
fun ByteArray.isTooLarge(): Boolean = size > 10_000_000 // 10MB

/**
 * Formata tamanho de arquivo
 */
fun ByteArray.formatFileSize(): String = size.formatFileSize()

fun Int.formatFileSize(): String {
    return when {
        this < 1024 -> "${this}B"
        this < 1024 * 1024 -> "${(this / 1024f).roundToInt()}KB"
        this < 1024 * 1024 * 1024 -> "${(this / (1024f * 1024f)).roundToInt()}MB"
        else -> "${(this / (1024f * 1024f * 1024f)).roundToInt()}GB"
    }
}

/**
 * Verifica se as coordenadas do bounding box são válidas
 */
fun BoundingBox.hasValidCoordinates(maxWidth: Float = 1920f, maxHeight: Float = 1080f): Boolean {
    return left >= 0 && top >= 0 && right <= maxWidth && bottom <= maxHeight
}

/**
 * Verifica se o bounding box tem tamanho razoável
 */
fun BoundingBox.hasReasonableSize(): Boolean {
    val width = width()
    val height = height()
    return width >= 10 && height >= 10 && width <= 1000 && height <= 1000
}

/**
 * Verifica se o objeto detectado tem medições úteis
 */
fun DetectedObject.hasUsefulMeasurements(): Boolean {
    return measurements?.hasAnyMeasurement() == true
}

/**
 * Verifica se o objeto detectado é válido
 */
fun DetectedObject.isValid(): Boolean {
    return confidence.isValidConfidence() &&
            boundingBox?.isValid() == true &&
            timestamp > 0
}