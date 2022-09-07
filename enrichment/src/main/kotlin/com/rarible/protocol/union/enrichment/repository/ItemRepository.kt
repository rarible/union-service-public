package com.rarible.protocol.union.enrichment.repository

import com.mongodb.client.result.DeleteResult
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.mongo.util.div
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lte
import org.springframework.stereotype.Component
import java.time.Instant

@Component
@CaptureSpan(type = SpanType.DB)
class ItemRepository(
    private val template: ReactiveMongoTemplate
) {

    private val logger = LoggerFactory.getLogger(ItemRepository::class.java)

    val collection: String = template.getCollectionName(ShortItem::class.java)

    suspend fun createIndices() {
        ALL_INDEXES.forEach { index ->
            logger.info("Ensure index '{}' for collection '{}'", index, collection)
            template.indexOps(collection).ensureIndex(index).awaitFirst()
        }
    }

    suspend fun save(item: ShortItem): ShortItem {
        return template.save(item).awaitFirst()
    }

    suspend fun get(id: ShortItemId): ShortItem? {
        return template.findById<ShortItem>(id).awaitFirstOrNull()
    }

    suspend fun getAll(ids: List<ShortItemId>): List<ShortItem> {
        val criteria = Criteria("_id").inValues(ids)
        return template.find<ShortItem>(Query(criteria)).collectList().awaitFirst()
    }

    fun findWithMultiCurrency(lastUpdateAt: Instant): Flow<ShortItem> {
        val query = Query(
            Criteria().andOperator(
                ShortItem::multiCurrency isEqualTo true,
                ShortItem::lastUpdatedAt lte lastUpdateAt
            )
        ).withHint(MULTI_CURRENCY_DEFINITION.indexKeys)

        return template.find(query, ShortItem::class.java).asFlow()
    }

    fun findByBlockchain(
        fromShortItemId: ShortItemId?, blockchain: BlockchainDto?, limit: Int
    ): Flow<ShortItem> {
        val criteria = Criteria().andOperator(
            listOfNotNull(
                blockchain?.let { ShortItem::blockchain isEqualTo it },
                fromShortItemId?.let { Criteria.where("_id").gt(it) }
            )
        )

        val query = Query(criteria)
            .with(Sort.by("_id"))
            .limit(limit)
        return template.find(query, ShortItem::class.java).asFlow()
    }

    fun findByAuction(auctionId: AuctionIdDto): Flow<ShortItem> {
        val query = Query(ShortItem::auctions isEqualTo auctionId)
        return template.find(query, ShortItem::class.java).asFlow()
    }

    fun findByPlatformWithSell(
        platform: PlatformDto,
        fromItemId: ShortItemId?,
        limit: Int?
    ): Flow<ShortItem> {
        val criteria = Criteria().andOperator(
            listOfNotNull(
                ShortItem::bestSellOrder / ShortOrder::platform isEqualTo platform.name,
                fromItemId?.let { Criteria.where("_id").gt(it) }
            )
        )
        val query = Query(criteria)
            .with(Sort.by("${ShortItem::bestSellOrder.name}.${ShortOrder::platform.name}", "_id"))
            .withHint(BY_BEST_SELL_PLATFORM_DEFINITION.indexKeys)

        limit?.let { query.limit(it) }

        return template.find(query, ShortItem::class.java).asFlow()
    }

    suspend fun delete(itemId: ShortItemId): DeleteResult? {
        val criteria = Criteria("_id").isEqualTo(itemId)
        return template.remove(Query(criteria), collection).awaitFirstOrNull()
    }

    companion object {

        private val BLOCKCHAIN_DEFINITION = Index()
            .on(ShortItem::blockchain.name, Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .background()

        private val MULTI_CURRENCY_DEFINITION = Index()
            .on(ShortItem::multiCurrency.name, Sort.Direction.DESC)
            .on(ShortItem::lastUpdatedAt.name, Sort.Direction.DESC)
            .background()

        private val BY_BEST_SELL_PLATFORM_DEFINITION = Index()
            .on("${ShortItem::bestSellOrder.name}.${ShortOrder::platform.name}", Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .background()

        private val AUCTION_DEFINITION = Index()
            .on(ShortItem::auctions.name, Sort.Direction.DESC)
            .background()

        private val ALL_INDEXES = listOf(
            BLOCKCHAIN_DEFINITION,
            MULTI_CURRENCY_DEFINITION,
            BY_BEST_SELL_PLATFORM_DEFINITION,
            AUCTION_DEFINITION
        )
    }
}
