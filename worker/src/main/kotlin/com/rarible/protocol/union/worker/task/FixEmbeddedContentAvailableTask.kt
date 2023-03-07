package com.rarible.protocol.union.worker.task

import com.rarible.core.common.optimisticLock
import com.rarible.core.task.RunTask
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.enrichment.meta.embedded.EmbeddedContentUrlProvider
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentItemEventService
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.flow.flow
import org.springframework.stereotype.Component

@Deprecated("remove after release")
@Component
class FixEmbeddedContentAvailableTask(
    private val itemRepository: ItemRepository,
    private val itemService: EnrichmentItemService,
    private val embeddedContentUrlProvider: EmbeddedContentUrlProvider,
    private val enrichmentItemEventService: EnrichmentItemEventService,
) : TaskHandler<String> {

    override val type = "FIX_EMBEDDED_CONTENT_AVAILABLE_TASK"

    override fun getAutorunParams(): List<RunTask> {
        return listOf(RunTask(param = ""))
    }

    override fun runLongTask(from: String?, param: String): Flow<String> = flow {
        itemRepository.findAll(fromIdExcluded = from?.let { ShortItemId.of(from) }).collectIndexed { index, item ->
            optimisticLock {
                val forUpdate = itemRepository.get(item.id)
                if (forUpdate?.metaEntry?.data != null) {
                    val data = forUpdate.metaEntry!!.data!!
                    val updatedData = data.copy(content = data.content.map {
                        if (embeddedContentUrlProvider.isEmbeddedContentUrl(it.url) &&
                            it.properties?.available == false
                        ) {
                            it.copy(properties = it.properties?.withAvailable(true))
                        } else {
                            it
                        }
                    })
                    if (data != updatedData) {
                        itemRepository.save(
                            forUpdate.copy(
                                metaEntry = forUpdate.metaEntry?.copy(
                                    data = updatedData
                                )
                            )
                        )
                        enrichmentItemEventService.onItemUpdated(
                            UnionItemUpdateEvent(item = itemService.fetch(item.id), eventTimeMarks = null)
                        )
                    }
                }
            }
            if (index % 1000 == 0) {
                emit(item.id.toString())
            }
        }
    }
}