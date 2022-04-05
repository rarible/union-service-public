package com.rarible.protocol.union.search.core

import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.search.core.repository.ActivityEsRepository
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.Instant

@Document(indexName = ActivityEsRepository.INDEX)
data class ElasticActivity(
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
    val user: User,
    val collection: Collection,
    val item: Item,
) {
    data class User(
        val maker: String,
        val taker: String?,
    )

    data class Collection(
        val make: String,
        val take: String?
    )

    data class Item(
        val make: String,
        val take: String?
    )
}
