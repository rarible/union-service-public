package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinition
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig.INDEX_SETTINGS
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig.loadMapping
import org.springframework.data.annotation.Id

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

    companion object {
        const val NAME = "collection"
        private const val REINDEX_TASK_NAME = "COLLECTION_REINDEX"
        private const val VERSION = 1
        val ENTITY_DEFINITION = EntityDefinition(
            name = NAME,
            mapping = loadMapping(NAME),
            versionData = VERSION,
            settings = INDEX_SETTINGS,
            reindexTaskName = REINDEX_TASK_NAME
        )
    }
}
