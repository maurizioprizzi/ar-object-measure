package com.objectmeasure.ar.domain.repository

import com.objectmeasure.ar.domain.model.DetectedObject
import com.objectmeasure.ar.domain.model.ObjectType
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface para operações com objetos detectados
 * DIA 2: Contrato básico para detecção e medição
 *
 * Esta interface define o contrato que será implementado na data layer
 */
interface ObjectRepository {

    /**
     * Detecta objetos em uma imagem
     * @param imageData dados da imagem capturada
     * @return Flow com lista de objetos detectados
     */
    suspend fun detectObjects(imageData: ByteArray): Flow<Result<List<DetectedObject>>>

    /**
     * Mede um objeto específico baseado em suas coordenadas
     * @param boundingBox coordenadas do objeto na imagem
     * @param objectType tipo do objeto para medição específica
     * @return resultado da medição
     */
    suspend fun measureObject(
        boundingBox: BoundingBox,
        objectType: ObjectType
    ): Result<DetectedObject>

    /**
     * Obtém histórico de objetos detectados
     * @param limit número máximo de objetos a retornar
     * @return lista dos objetos detectados recentemente
     */
    suspend fun getDetectionHistory(limit: Int = 10): Result<List<DetectedObject>>

    /**
     * Salva um objeto detectado no cache/database
     * @param detectedObject objeto a ser salvo
     * @return sucesso ou falha da operação
     */
    suspend fun saveDetectedObject(detectedObject: DetectedObject): Result<Unit>

    /**
     * Verifica se a câmera AR está disponível
     * @return true se AR está disponível e configurado
     */
    suspend fun isARAvailable(): Boolean
}

/**
 * Representa as coordenadas de um objeto na imagem
 * DIA 2: Estrutura básica para bounding box
 */
data class BoundingBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    /**
     * Calcula a largura do bounding box
     */
    fun width(): Float = right - left

    /**
     * Calcula a altura do bounding box
     */
    fun height(): Float = bottom - top

    /**
     * Calcula a área do bounding box
     */
    fun area(): Float = width() * height()

    /**
     * Verifica se o bounding box é válido
     */
    fun isValid(): Boolean {
        return left >= 0 && top >= 0 && right > left && bottom > top
    }

    /**
     * Retorna o centro do bounding box
     */
    fun center(): Pair<Float, Float> {
        return Pair((left + right) / 2, (top + bottom) / 2)
    }
}