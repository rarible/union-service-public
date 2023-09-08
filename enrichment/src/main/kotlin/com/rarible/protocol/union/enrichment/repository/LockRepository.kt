package com.rarible.protocol.union.enrichment.repository

import com.rarible.core.common.nowMillis
import com.rarible.core.common.optimisticLock
import com.rarible.protocol.union.enrichment.model.Lock
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class LockRepository(
    private val template: ReactiveMongoTemplate
) {

    suspend fun acquireLock(id: String, resetAfter: Duration): Boolean {
        return optimisticLock {
            val now = nowMillis()
            val lock = getOrDefault(id)
            if (lock.acquired && Duration.between(lock.acquiredAt, now).compareTo(resetAfter) == -1) {
                false
            } else {
                val updated = lock.copy(acquired = true, acquiredAt = nowMillis())
                template.save(updated).awaitSingle()
                true
            }
        }
    }

    suspend fun releaseLock(id: String) {
        template.save(getOrDefault(id).copy(acquired = false)).awaitSingle()
    }

    suspend fun get(id: String): Lock? {
        return template.findById<Lock>(id).awaitSingleOrNull()
    }

    private suspend fun getOrDefault(id: String): Lock {
        return get(id) ?: Lock(id)
    }
}
