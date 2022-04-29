package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinition
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig.INDEX_SETTINGS
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig.loadMapping
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.Instant

data class EsActivity(
    @Id
    val activityId: String, // blockchain:value
    // Sort fields
    @Field(type = FieldType.Date)
    val date: Instant,
    val blockNumber: Long?,
    val logIndex: Int?,
    val salt: Long = kotlin.random.Random.nextLong(),
    // Filter fields
    val blockchain: BlockchainDto,
    val type: ActivityTypeDto,
    val userFrom: String?,
    val userTo: String?,
    val collection: String?,
    val item: String,
) {
    companion object {
        const val NAME = "activity"
        const val VERSION: Int = 2
        private const val REINDEX_TASK_NAME = "ACTIVITY_REINDEX"
        val ENTITY_DEFINITION = EntityDefinition(
            name = NAME,
            mapping = loadMapping(NAME),
            versionData = VERSION,
            settings = INDEX_SETTINGS,
            REINDEX_TASK_NAME
        )
    }
}
