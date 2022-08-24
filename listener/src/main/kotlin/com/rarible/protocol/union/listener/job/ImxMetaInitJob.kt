package com.rarible.protocol.union.listener.job

import com.rarible.loader.cache.internal.CacheRepository
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemMetaEvent
import com.rarible.protocol.union.core.model.UnionItemMetaRefreshEvent
import com.rarible.protocol.union.integration.immutablex.service.ImxItemService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
@Deprecated("Only for launch, remove later")
class ImxMetaInitJob(
    private val cacheRepository: CacheRepository,
    private val imxItemService: ImxItemService,
    private val itemMetaHandler: IncomingEventHandler<UnionItemMetaEvent>,
    @Value("\${listener.immutablex-meta-task.delay:1000}")
    private val delay: Long,
    @Value("\${listener.immutablex-meta-task.enabled:false}")
    private val enabled: Boolean
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun execute(continuation: String?): Flow<String> {
        if (!enabled) return emptyFlow()

        return flow {
            var next = continuation
            do {
                next = scheduleMeta(next)
                if (next != null) {
                    emit(next)
                }
            } while (next != null)
        }
    }

    private suspend fun scheduleMeta(continuation: String?): String? {
        val page = imxItemService.getAllItems(continuation, 200, false, null, null)
        val items = page.entities
        if (items.isEmpty()) return null

        items.forEach {
            itemMetaHandler.onEvent(UnionItemMetaRefreshEvent(it.id))
        }
        logger.info("Sent {} Immutablex meta refresh tasks", items.size)
        if (delay > 0) {
            delay(delay)
        }
        return page.continuation
    }

}