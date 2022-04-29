package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinition
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig.INDEX_SETTINGS
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig.loadMapping
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.UnionAddress
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import java.time.Instant

class EsOrder(
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

    companion object {
        const val NAME = "order"
        private const val REINDEX_TASK_NAME = "ORDER_REINDEX"
        private const val VERSION = 1
        val ENTITY_DEFINITION = EntityDefinition(
            name = NAME,
            mapping = loadMapping(NAME),
            versionData = VERSION,
            settings = INDEX_SETTINGS,
            reindexTaskName = REINDEX_TASK_NAME
        )
    }
}
