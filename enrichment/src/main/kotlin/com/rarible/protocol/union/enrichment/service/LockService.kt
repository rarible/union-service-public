package com.rarible.protocol.union.enrichment.service

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.util.spent
import com.rarible.protocol.unlockable.api.client.LockControllerApi
import kotlinx.coroutines.reactive.awaitFirstOrDefault
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class LockService(
    private val lockControllerApi: LockControllerApi
) {

    private val logger = LoggerFactory.getLogger(LockService::class.java)

    suspend fun isUnlockable(itemId: ShortItemId): Boolean {
        val now = nowMillis()
        val result = lockControllerApi
            .isUnlockable(itemId.toDto().value)
            .awaitFirstOrDefault(false)
        logger.info("Fetched Unlockable marker for Item [{}]: [{}] ({}ms)", itemId, result, spent(now))
        return result
    }

}