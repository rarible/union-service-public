package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.Instant

@Document(indexName = "ownership", createIndex = false)
data class EsOwnership(
    @Id
    val ownershipId: String,
    val blockchain: BlockchainDto,
    val itemId: String? = null,
    val collection: String? = null,
    val owner: String,
    @Field(type = FieldType.Date)
    val date: Instant,
)
