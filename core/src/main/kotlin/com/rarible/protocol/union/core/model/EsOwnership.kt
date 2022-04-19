package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.BlockchainDto
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import java.time.Instant

@Document(indexName = "ownership", createIndex = false)
class EsOwnership(
    @Id
    val ownershipId: String,
    val blockchain: BlockchainDto,
    val token: String? = null,
    val tokenId: String? = null,
    val itemId: String? = null,
    val collection: String? = null,
    val owner: String,
    val value: String,
    val date: Instant,
    val price: String?,
    val priceUsd: String?,
    val auctionEndDate: Instant?,
    val orderSource: String?,
)
