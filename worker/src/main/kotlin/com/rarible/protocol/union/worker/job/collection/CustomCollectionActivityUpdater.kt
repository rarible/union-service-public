package com.rarible.protocol.union.worker.job.collection

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.enrichment.converter.EnrichmentActivityDtoConverter
import com.rarible.protocol.union.enrichment.repository.ActivityRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentActivityService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CustomCollectionActivityUpdater(
    private val router: BlockchainRouter<ActivityService>,
    private val enrichmentActivityService: EnrichmentActivityService,
    private val eventProducer: RaribleKafkaProducer<ActivityDto>,
    private val activityRepository: ActivityRepository,
    private val featureFlagsProperties: FeatureFlagsProperties,
) : CustomCollectionUpdater {

    private val batchSize = 200

    private val logger = LoggerFactory.getLogger(javaClass)

    private val activityTypes = listOf(
        ActivityTypeDto.TRANSFER,
        ActivityTypeDto.MINT,
        ActivityTypeDto.BURN,
        ActivityTypeDto.BID,
        ActivityTypeDto.LIST,
        ActivityTypeDto.SELL,
        ActivityTypeDto.CANCEL_LIST,
        ActivityTypeDto.CANCEL_BID
    )

    override suspend fun update(item: UnionItem) {
        val service = router.getService(item.id.blockchain)
        var continuation: String? = null
        do {
            val page = service.getActivitiesByItem(
                types = activityTypes,
                itemId = item.id.value,
                continuation = continuation,
                size = batchSize,
                sort = ActivitySortDto.EARLIEST_FIRST
            )

            val messages = if (featureFlagsProperties.enableMongoActivityWrite) {
                enrichmentActivityService.enrich(page.entities.filter { it.reverted != true })
                    .map {
                        activityRepository.save(it)
                        KafkaEventFactory.activityEvent(EnrichmentActivityDtoConverter.convert(it))
                    }
            } else {
                enrichmentActivityService.enrichDeprecated(page.entities.filter { it.reverted != true })
                    .map { KafkaEventFactory.activityEvent(it) }
            }


            eventProducer.send(messages)

            logger.info("Updated {} activities for custom collection migration of Item {}", messages.size, item.id)

            continuation = page.continuation
        } while (continuation != null)
    }

}