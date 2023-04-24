package com.rarible.protocol.union.worker.job.collection

import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionOwnershipChangeEvent
import com.rarible.protocol.union.core.model.offchainEventMark
import com.rarible.protocol.union.core.producer.UnionInternalBlockchainEventProducer
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import kotlinx.coroutines.flow.collect
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CustomCollectionOwnershipUpdater(
    private val router: BlockchainRouter<OwnershipService>,
    private val eventProducer: UnionInternalBlockchainEventProducer,
) : CustomCollectionUpdater {

    private val batchSize = 200

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun update(item: UnionItem) {
        val service = router.getService(item.id.blockchain)
        var continuation: String? = null
        do {
            val page = service.getOwnershipsByItem(
                itemId = item.id.value,
                continuation = continuation,
                size = batchSize
            )

            val eventTimeMarks = offchainEventMark("enrichment-in")
            val messages = page.entities.map {
                KafkaEventFactory.internalOwnershipEvent(
                    UnionOwnershipChangeEvent(it.id, eventTimeMarks)
                )
            }
            eventProducer.getProducer(item.id.blockchain).send(messages).collect()

            logger.info("Updated {} ownerships for custom collection migration of Item {}", messages.size, item.id)

            continuation = page.continuation
        } while (continuation != null)
    }

}