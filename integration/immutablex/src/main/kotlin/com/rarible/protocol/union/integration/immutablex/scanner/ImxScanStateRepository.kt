package com.rarible.protocol.union.integration.immutablex.scanner

import com.rarible.core.common.nowMillis
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import java.time.Instant

class ImxScanStateRepository(
    private val mongo: ReactiveMongoTemplate,
) {

    suspend fun getState(type: ImxScanEntityType): ImxScanState? {
        val id = type.name.lowercase()
        return mongo.findById(id, ImxScanState::class.java).awaitFirstOrNull()
    }

    suspend fun createState(type: ImxScanEntityType, entityId: String): ImxScanState {
        val id = type.name.lowercase()
        return mongo.save(
            ImxScanState(
                id = id,
                lastDate = nowMillis(),
                entityDate = nowMillis(),
                entityId = entityId
            )
        ).awaitFirst()
    }

    suspend fun updateState(state: ImxScanState, entityDate: Instant, entityId: String) {
        mongo.save(
            state.copy(
                lastDate = nowMillis(),
                entityDate = entityDate,
                entityId = entityId
            )
        ).awaitFirstOrNull()
    }

    suspend fun updateState(state: ImxScanState, error: Exception) {
        mongo.save(
            state.copy(
                lastDate = nowMillis(),
                lastError = error.message,
                lastErrorDate = nowMillis(),
                lastErrorStacktrace = error.stackTraceToString()
            )
        ).awaitFirstOrNull()
    }

}