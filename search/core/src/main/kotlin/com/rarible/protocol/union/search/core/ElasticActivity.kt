package com.rarible.protocol.union.search.core

import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.search.core.repository.ActivityEsRepository
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.Instant
import java.util.*

@Document(indexName = ActivityEsRepository.INDEX)
data class ElasticActivity(
    val activityId: String,
    // Sort fields
    @Field(type = FieldType.Date)
    val date: Instant,
    val blockNumber: Long?,
    val logIndex: Int?,
    // Filter fields
    val blockchain: BlockchainDto,
    val type: ActivityTypeDto,
    val user: User,
    val collection: Collection,
    val item: Item,

    @Id
    val uuid: UUID = UUID.randomUUID(),
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
