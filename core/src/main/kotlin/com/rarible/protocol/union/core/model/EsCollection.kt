package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.core.model.elasticsearch.EsEntity
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinition
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig.INDEX_SETTINGS
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig.loadMapping
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.data.annotation.Id

sealed class EsCollectionSealed {
    abstract val collectionId: String // blockchain:value
}

data class EsCollectionLite(
    override val collectionId: String, //blockchain:value
) : EsCollectionSealed()

data class EsCollection(
    @Id
    override val collectionId: String, //blockchain:value

    val blockchain: BlockchainDto,
    val name: String,
    val symbol: String? = null,
    val owner: String? = null,
    val meta: CollectionMeta? = null,
) : EsCollectionSealed() {

    data class CollectionMeta(
        val name: String,
        val description: String? = null,
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
