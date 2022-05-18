package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.core.model.elasticsearch.EsEntity
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinition
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig.INDEX_SETTINGS
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig.loadMapping
import org.springframework.data.annotation.Id

data class EsCollection(
    @Id
    val collectionId: String,
    val type: String,
    val blockchain: String,
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

    companion object {
        private const val VERSION = 1

        val ENTITY_DEFINITION = EsEntity.COLLECTION.let {
            EntityDefinition(
                entity = it,
                mapping = loadMapping(it),
                versionData = VERSION,
                settings = INDEX_SETTINGS
            )
        }
    }
}
