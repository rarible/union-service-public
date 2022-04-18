package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.Instant

@Document(indexName = "activity", createIndex = false)
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
}
