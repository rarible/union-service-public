package com.rarible.protocol.union.core.model

import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document

@Document(indexName = "collection", createIndex = false)
data class EsCollection(
    @Id
    val collectionId: String,
    val type: String,
    val name: String,
    val symbol: String? = null,
    val owner: String? = null,
    val meta: CollectionMeta? = null,
    val parent: String? = null
) {

    data class CollectionMeta(
        val name: String,
        val description: String? = null,
        val feeRecipient: String? = null
    )
}


