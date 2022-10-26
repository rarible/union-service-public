package com.rarible.protocol.union.enrichment.migration

import com.github.cloudyrock.mongock.ChangeLog
import com.github.cloudyrock.mongock.ChangeSet
import io.changock.migration.api.annotations.NonLockGuarded
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.springframework.data.mongodb.core.ReactiveMongoTemplate

@ChangeLog(order = "1")
class ChangeLog00002DeleteLegacyCollections {

    @ChangeSet(
        id = "ChangeLog00002DeleteLegacyCollections.deleteLegacyCollections",
        order = "1",
        author = "protocol"
    )
    fun deleteLegacyCollections(
        @NonLockGuarded template: ReactiveMongoTemplate
    ) = runBlocking {

        val collections = listOf(
            // Legacy item/ownership collections, replaced by enrichment_item/enrichment_ownership
            "item",
            "ownership",
            // Legacy meta collection we used to migrate meta from ETH to Union
            "cache_meta",
            // Legacy reconciliation mark collections, replaced by generic reconciliation_mark
            "item_reconciliation_mark",
            "ownership_reconciliation_mark"
        )

        collections.forEach {
            template.dropCollection(it).awaitFirstOrNull()
        }

    }
}