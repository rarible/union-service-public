package com.rarible.protocol.union.integration.immutablex.scanner

import com.rarible.core.common.nowMillis
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import java.time.Instant

class ImxScanStateRepository(
    private val mongo: ReactiveMongoTemplate,
) {

    suspend fun getOrCreateState(type: ImxScanEntityType): ImxScanState {
        val id = type.name
        return mongo.findById(id, ImxScanState::class.java).awaitFirstOrNull()
            ?: mongo.save(ImxScanState(id = id)).awaitFirst()
    }

    suspend fun updateState(state: ImxScanState, entityDate: Instant?, entityId: String?) {
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