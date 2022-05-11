package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.core.model.elasticsearch.EsEntity
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinition
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig.INDEX_SETTINGS
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig.loadMapping
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.Instant

data class EsItem(
    @Id
    val itemId: String,
    val blockchain: BlockchainDto,
    val collection: String,

    val name: String,
    val description: String,
    val traits: Map<String, String>,

    val creators: List<String>,
    val owner: String,

    @Field(type = FieldType.Date)
    val mintedAt: Instant,

    @Field(type = FieldType.Date)
    val lastUpdatedAt: Instant,
) {
    companion object {
        private const val VERSION: Int = 1

        val ENTITY_DEFINITION = EntityDefinition(
            entity = EsEntity.ITEM,
            mapping = loadMapping(EsEntity.ITEM),
            versionData = VERSION,
            settings = INDEX_SETTINGS
        )
    }
}
