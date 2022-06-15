package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.core.model.elasticsearch.EsEntity
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinition
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig.INDEX_SETTINGS
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig.loadMapping
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig.loadSettings
import com.rarible.protocol.union.core.model.elasticsearch.generateSalt
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.Instant

sealed class EsCollectionSealed {
    abstract val collectionId: String // blockchain:value
    abstract val date: Instant
    abstract val salt: Long
}

data class EsCollectionLite(
    override val collectionId: String, //blockchain:value
    override val date: Instant,
    override val salt: Long,
) : EsCollectionSealed()

data class EsCollection(
    @Id
    override val collectionId: String, //blockchain:value

    // Sort fields
    @Field(type = FieldType.Date)
    override val date: Instant,
    override val salt: Long = generateSalt(),
    // Filter fields
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
                settings = loadSettings(it)
            )
        }
    }
}
