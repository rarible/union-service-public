package com.rarible.protocol.union.search.indexer.handler

import com.rarible.core.common.mapAsync
import com.rarible.core.daemon.sequential.ConsumerBatchEventHandler
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.core.converter.EsActivityConverter
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.service.ItemService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import org.springframework.stereotype.Service

@Service
class ActivityEventHandler(
    private val repository: EsActivityRepository,
    private val router: BlockchainRouter<ItemService>,
): ConsumerBatchEventHandler<ActivityDto> {

    companion object {
        private val logger by Logger()
    }
    override suspend fun handle(event: List<ActivityDto>) {
        logger.info("Handling ${event.size} ActivityDto events")

        var convertedEvents = event.mapNotNull {
            logger.debug("Converting ActivityDto id = ${it.id}")
            EsActivityConverter.convert(it)
        }

        convertedEvents = fillCollections(convertedEvents)

        repository.saveAll(convertedEvents)
        logger.info("Handling completed")
    }

    private suspend fun fillCollections(activities: List<EsActivity>): List<EsActivity> {
        val activitiesByBlockchain = activities.groupBy(EsActivity::blockchain)

        val items = activitiesByBlockchain.mapAsync { (blockchain, activities) ->
            val itemIds = activities.map { blockchain.name + ":" + it.item }
            router.getService(blockchain).getItemsByIds(itemIds)
        }.flatten()

        val itemsIdMapping = items.associateBy { it.id.value }

        return activities.map {
            val collection = itemsIdMapping[it.item]?.collection?.value
            if (collection != null) {
                it.copy(collection = collection)
            } else it
        }
    }
}
