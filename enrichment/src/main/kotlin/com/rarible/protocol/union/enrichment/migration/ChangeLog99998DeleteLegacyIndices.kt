package com.rarible.protocol.union.enrichment.migration

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

@ChangeLog(order = "99998")
class ChangeLog99998DeleteLegacyIndices {

    private val legacyIndices = mapOf(
        ShortItem.COLLECTION to listOf(
            "metaEntry.retries_1_metaEntry.retriedAt_1",

            "auctions_-1",
            "collectionId_1__id_1",
            // TODO uncomment after new indices have been created
            // "multiCurrency_-1_lastUpdatedAt_-1",
            // "poolSellOrders.order._id_1_blockchain_1__id_1",
            // "bestSellOrder.platform_1__id_1",
        ),
        ShortOwnership.COLLECTION to listOf(
            // TODO uncomment after new indices have been created
            // "multiCurrency_-1_lastUpdatedAt_-1",
            // "bestSellOrder.platform_1__id_1",
        )
    )

    @ChangeSet(
        id = "ChangeLog99998DeleteLegacyIndices.dropLegacyIndices",
        order = "99998",
        author = "protocol",
        runAlways = true
    )
    fun dropLegacyIndices(
        @NonLockGuarded
        template: ReactiveMongoTemplate
    ) = runBlocking {
        legacyIndices.forEach { (collection, indices) ->
            val exists = template.indexOps(collection).indexInfo.map { it.name }.collectList().awaitSingle().toSet()
            indices.filter { exists.contains(it) }.forEach { index ->
                template.indexOps(collection).dropIndex(index).awaitSingleOrNull()
            }
        }
    }
}
