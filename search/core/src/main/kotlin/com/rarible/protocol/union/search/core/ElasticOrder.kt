package com.rarible.protocol.union.search.core

import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.search.core.repository.OrderEsRepository
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.Instant


@Document(indexName = OrderEsRepository.INDEX, createIndex = false)
class ElasticOrder(
    @Id
    val orderId: String, // blockchain:value

    @Field(type = FieldType.Date)
    val lastUpdatedAt: Instant,

    val type: Type,
    val blockchain: BlockchainDto,
    val platform: PlatformDto,
    val maker: UnionAddress,
    val make: Asset,
    val taker: UnionAddress?,
    val take: Asset,
    val start: Instant?,
    val end: Instant?,
    val origins: List<UnionAddress>,
    val status: OrderStatusDto
) {

    data class Asset(
        val type: AssetTypeDto
    )

    enum class Type {
        SELL, BID
    }

}
