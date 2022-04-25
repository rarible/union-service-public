package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinition
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig.INDEX_SETTINGS
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig.loadMapping
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.Instant

data class EsOwnership(
    @Id
    val ownershipId: String,
    val blockchain: BlockchainDto,
    val itemId: String? = null,
    val collection: String? = null,
    val owner: String,
    @Field(type = FieldType.Date)
    val date: Instant,
) {
    companion object {
        const val NAME = "ownership"
        private const val REINDEX_TASK_NAME = "OWNERSHIP_REINDEX"
        private const val VERSION: Int = 1
        val ENTITY_DEFINITION =
            EntityDefinition(
                name = NAME,
                mapping = loadMapping(NAME),
                versionData = VERSION,
                settings = INDEX_SETTINGS,
                reindexTaskName = REINDEX_TASK_NAME
            )
    }
}
