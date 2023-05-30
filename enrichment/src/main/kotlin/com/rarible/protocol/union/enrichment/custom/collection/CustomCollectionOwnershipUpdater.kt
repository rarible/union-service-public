package com.rarible.protocol.union.enrichment.custom.collection

import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.producer.UnionInternalOwnershipEventProducer
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CustomCollectionOwnershipUpdater(
    private val router: BlockchainRouter<OwnershipService>,
    private val eventProducer: UnionInternalOwnershipEventProducer,
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

            eventProducer.sendChangeEvents(page.entities.map { it.id })

            logger.info("Updated {} ownerships for custom collection migration of Item {}", page.entities.size, item.id)

            continuation = page.continuation
        } while (continuation != null)
    }

}