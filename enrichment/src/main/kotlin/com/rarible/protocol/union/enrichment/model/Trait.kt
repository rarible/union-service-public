package com.rarible.protocol.union.enrichment.model

import com.rarible.protocol.union.enrichment.util.TraitUtils
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

@Document(Trait.COLLECTION)
data class Trait(
    val collectionId: EnrichmentCollectionId,
    val key: String,
    val value: String?,
    val itemsCount: Long,
    val listedItemsCount: Long = 0,
    val version: Long = 0,
    /** id: collectionId:hash(key):hash(value) */
    @Id
    val id: String = TraitUtils.getId(collectionId, key, value),
) {
    companion object {
        const val COLLECTION = "trait"
    }
}
