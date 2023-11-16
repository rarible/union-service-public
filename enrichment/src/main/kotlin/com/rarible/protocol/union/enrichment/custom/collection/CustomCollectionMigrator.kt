package com.rarible.protocol.union.enrichment.custom.collection

import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.producer.UnionInternalItemEventProducer
import com.rarible.protocol.union.enrichment.custom.collection.updater.CustomCollectionOrderUpdater
import com.rarible.protocol.union.enrichment.custom.collection.updater.CustomCollectionUpdater
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class CustomCollectionMigrator(
    private val eventProducer: UnionInternalItemEventProducer,
    private val updaters: List<CustomCollectionUpdater>,
    private val ff: FeatureFlagsProperties
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val enabledUpdaters = updaters.filterNot {
        ff.skipOrderMigration && it is CustomCollectionOrderUpdater
    }

    // Here we need just to send events, collections will be replaced in listener
    suspend fun migrate(items: List<UnionItem>) = coroutineScope {
        if (items.isEmpty()) {
            return@coroutineScope
        }
        // TODO ideally, there should be check - if collection already substituted, but it is possible
        // only if we have items in Union
        items.map { item ->
            async {
                eventProducer.sendChangeEvent(item.id)
                enabledUpdaters.map { async { it.update(item) } }.awaitAll()
            }
        }.awaitAll()

        logger.info("Custom collection recalculated for Items: {}", items.map { it.id.fullId() })
    }
}
