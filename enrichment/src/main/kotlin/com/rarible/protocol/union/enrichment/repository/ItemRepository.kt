package com.rarible.protocol.union.enrichment.repository

import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.core.mongo.util.div
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.enrichment.download.DownloadEntry
import com.rarible.protocol.union.enrichment.download.DownloadStatus
import com.rarible.protocol.union.enrichment.model.ShortDateIdItem
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactive.awaitSingle
import org.bson.Document
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.index.PartialIndexFilter
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.and
import org.springframework.data.mongodb.core.query.gt
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lt
import org.springframework.data.mongodb.core.query.lte
import org.springframework.stereotype.Component
import java.time.Duration
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
            template.indexOps(collection).ensureIndex(index).awaitSingle()
        }
    }

    suspend fun save(item: ShortItem): ShortItem {
        return template.save(item).awaitFirst()
    }

    suspend fun get(id: ShortItemId): ShortItem? {
        return template.findById<ShortItem>(id).awaitFirstOrNull()
    }

    suspend fun getAll(ids: List<ShortItemId>): List<ShortItem> {
        if (ids.isEmpty()) return emptyList()
        val criteria = Criteria("_id").inValues(ids)
        return template.find<ShortItem>(Query(criteria)).collectList().awaitFirst()
    }

    fun getItemForMetaRetry(
        now: Instant,
        retryPeriod: Duration,
        attempt: Int,
        status: DownloadStatus
    ): Flow<ShortItem> {
        val query = Query(
            Criteria().andOperator(
                ShortItem::metaEntry / DownloadEntry<*>::status isEqualTo status,
                ShortItem::metaEntry / DownloadEntry<*>::retries isEqualTo attempt,
                ShortItem::metaEntry / DownloadEntry<*>::retriedAt lt now.minus(retryPeriod)
            )
        )

        return template.find(query, ShortItem::class.java).asFlow()
    }

    fun findWithMultiCurrency(lastUpdateAt: Instant): Flow<ShortItem> {
        val query = Query(
            Criteria().andOperator(
                ShortItem::multiCurrency isEqualTo true,
                ShortItem::lastUpdatedAt lte lastUpdateAt
            )
        )

        return template.find(query, ShortItem::class.java).asFlow()
    }

    fun findByBlockchain(
        fromShortItemId: ShortItemId?,
        blockchain: BlockchainDto?,
        limit: Int
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

        limit?.let { query.limit(it) }

        return template.find(query, ShortItem::class.java).asFlow()
    }

    fun findByPoolOrder(blockchain: BlockchainDto, orderId: String): Flow<ShortItemId> {
        val criteria = Criteria
            .where(ShortItem::blockchain.name).isEqualTo(blockchain)
            .and(POOL_ORDER_ID_FIELD).isEqualTo(orderId)

        val query = Query(criteria)
        query.fields().include(ShortItem::itemId.name)

        return template.find(query, Document::class.java, collection).asFlow()
            .map { ShortItemId(blockchain, it.get(ShortItem::itemId.name) as String) }
    }

    suspend fun findIdsByLastUpdatedAt(
        lastUpdatedFrom: Instant,
        lastUpdatedTo: Instant,
        fromId: ShortItemId?,
        size: Int = 20
    ): List<ShortDateIdItem> {
        val criteria = if (fromId != null) {
            Criteria().orOperator(
                (ShortItem::lastUpdatedAt gt lastUpdatedFrom).lte(lastUpdatedTo),
                (ShortItem::lastUpdatedAt isEqualTo lastUpdatedFrom).and("_id").gt(fromId)
            )
        } else {
            (ShortItem::lastUpdatedAt gt lastUpdatedFrom).lte(lastUpdatedTo)
        }
        val query = Query
            .query(criteria)
            .with(Sort.by(ShortItem::lastUpdatedAt.name, ShortItem::id.name))
            .limit(size)

        query.fields()
            .include(ShortItem::id.name)
            .include(ShortItem::lastUpdatedAt.name)

        return template.find(query, ShortDateIdItem::class.java, ShortItem.COLLECTION)
            .collectList()
            .awaitFirst()
    }

    fun findAll(fromIdExcluded: ShortItemId? = null): Flow<ShortItem> = template.find(
        Query(
            Criteria().apply {
                fromIdExcluded?.let { and(ShortItem::id).gt(fromIdExcluded) }
            }
        ).with(Sort.by(ShortItem::id.name)),
        ShortItem::class.java
    ).asFlow()

    companion object {

        // Not really sure why, but Mongo saves id field of SHortOrder as _id
        private const val POOL_ORDER_ID_FIELD = "poolSellOrders.order._id"
        private const val BEST_SELL_PLATFORM_FIELD = "bestSellOrder.platform"

        private val STATUS_RETRIES_FAILED_AT_DEFINITION = Index()
            .on("${ShortItem::metaEntry.name}.${DownloadEntry<*>::status.name}", Sort.Direction.ASC)
            .on("${ShortItem::metaEntry.name}.${DownloadEntry<*>::retries.name}", Sort.Direction.ASC)
            .on("${ShortItem::metaEntry.name}.${DownloadEntry<*>::retriedAt.name}", Sort.Direction.ASC)
            .background()

        private val BLOCKCHAIN_DEFINITION = Index()
            .on(ShortItem::blockchain.name, Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .background()

        @Deprecated("Replace with MULTI_CURRENCY_DEFINITION")
        private val MULTI_CURRENCY_DEFINITION_LEGACY = Index()
            .on(ShortItem::multiCurrency.name, Sort.Direction.DESC)
            .on(ShortItem::lastUpdatedAt.name, Sort.Direction.DESC)
            .background()

        private val MULTI_CURRENCY_DEFINITION = Index()
            .partial(PartialIndexFilter.of(ShortItem::multiCurrency isEqualTo true))
            .on(ShortItem::lastUpdatedAt.name, Sort.Direction.DESC)
            // Originally we don't need it here, but without it there can be collisions in future
            // with other partial indices based on partial filter and lastUpdateAt only
            .on(ShortItem::multiCurrency.name, Sort.Direction.DESC)
            .background()

        @Deprecated("Replace with BY_BEST_SELL_PLATFORM_DEFINITION")
        private val BY_BEST_SELL_PLATFORM_DEFINITION_LEGACY = Index()
            .on(BEST_SELL_PLATFORM_FIELD, Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .background()

        // TODO findByPlatformWithSell should be updated before switch to this index
        private val BY_BEST_SELL_PLATFORM_DEFINITION = Index()
            .partial(PartialIndexFilter.of(Criteria.where(BEST_SELL_PLATFORM_FIELD).exists(true)))
            .on(BEST_SELL_PLATFORM_FIELD, Sort.Direction.ASC)
            .on(ShortItem::lastUpdatedAt.name, Sort.Direction.DESC)
            .on("_id", Sort.Direction.ASC)
            .background()

        @Deprecated("Replace with POOL_ORDER_DEFINITION")
        private val POOL_ORDER_DEFINITION_LEGACY = Index()
            .on(POOL_ORDER_ID_FIELD, Sort.Direction.ASC)
            .on(ShortItem::blockchain.name, Sort.Direction.ASC) // Just for case of orderId collision
            .on("_id", Sort.Direction.ASC)
            .sparse()
            .background()

        // TODO findByPoolOrder should be updated before switch to this index
        private val POOL_ORDER_DEFINITION = Index()
            .partial(PartialIndexFilter.of(Criteria.where(POOL_ORDER_ID_FIELD).exists(true)))
            .on(ShortItem::blockchain.name, Sort.Direction.ASC) // Just for case of orderId collision
            .on(POOL_ORDER_ID_FIELD, Sort.Direction.ASC)
            .background()

        private val LAST_UPDATED_AT_ID: Index = Index()
            .on(ShortItem::lastUpdatedAt.name, Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .background()

        private val ALL_INDEXES = listOf(
            STATUS_RETRIES_FAILED_AT_DEFINITION,
            BLOCKCHAIN_DEFINITION,
            MULTI_CURRENCY_DEFINITION,
            MULTI_CURRENCY_DEFINITION_LEGACY,
            BY_BEST_SELL_PLATFORM_DEFINITION,
            BY_BEST_SELL_PLATFORM_DEFINITION_LEGACY,
            POOL_ORDER_DEFINITION,
            POOL_ORDER_DEFINITION_LEGACY,
            LAST_UPDATED_AT_ID
        )
    }
}
