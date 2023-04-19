package com.rarible.protocol.union.worker.job.collection

import com.rarible.protocol.union.core.converter.EsActivityConverter
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentActivityService
import org.elasticsearch.action.support.WriteRequest
import org.springframework.stereotype.Component

@Component
class CustomCollectionActivityUpdater(
    private val router: BlockchainRouter<ActivityService>,
    private val repository: EsActivityRepository,
    private val converter: EsActivityConverter,
    private val enrichmentActivityService: EnrichmentActivityService
) : CustomCollectionUpdater {

    private val batchSize = 200

    override suspend fun update(item: UnionItem) {
        val service = router.getService(item.id.blockchain)
        var continuation: String? = null
        do {
            val page = service.getActivitiesByItem(
                types = emptyList(),
                itemId = item.id.value,
                continuation = continuation,
                size = batchSize,
                sort = ActivitySortDto.EARLIEST_FIRST
            )

            val dto = enrichmentActivityService.enrich(page.entities.filter { it.reverted != true })
            val toSave = converter.batchConvert(dto)

            repository.bulk(
                entitiesToSave = toSave,
                idsToDelete = emptyList(),
                refreshPolicy = WriteRequest.RefreshPolicy.NONE
            )

            continuation = page.continuation
        } while (continuation != null)
    }

}