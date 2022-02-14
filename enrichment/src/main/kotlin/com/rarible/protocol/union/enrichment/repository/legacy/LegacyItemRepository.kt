package com.rarible.protocol.union.enrichment.repository.legacy

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
import com.rarible.protocol.union.enrichment.model.legacy.LegacyShortItem
import com.rarible.protocol.union.enrichment.model.legacy.LegacyShortItemId
import com.rarible.protocol.union.enrichment.repository.ItemRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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
@Deprecated("Should be replaced by implementation without token/tokenId")
class LegacyItemRepository(
    private val template: ReactiveMongoTemplate
) : ItemRepository {

    private val logger = LoggerFactory.getLogger(LegacyItemRepository::class.java)

    val collection: String = template.getCollectionName(LegacyShortItem::class.java)

    override suspend fun createIndices() {
        ALL_INDEXES.forEach { index ->
            logger.info("Ensure index '{}' for collection '{}'", index, collection)
            template.indexOps(collection).ensureIndex(index).awaitFirst()
        }
    }

    override suspend fun save(item: ShortItem): ShortItem {
        val legacyItem = LegacyShortItem(item)
        return template.save(legacyItem).awaitFirst().toShortItem()
    }

    override suspend fun get(id: ShortItemId): ShortItem? {
        val legacyId = LegacyShortItemId(id)
        return template.findById<LegacyShortItem>(legacyId)
            .awaitFirstOrNull()
            ?.toShortItem()
    }

    override suspend fun getAll(ids: List<ShortItemId>): List<ShortItem> {
        val legacyIds = ids.map { LegacyShortItemId(it) }
        val criteria = Criteria("_id").inValues(legacyIds)
        return template.find<LegacyShortItem>(Query(criteria))
            .collectList()
            .awaitFirst()
            .map { it.toShortItem() }
    }

    override fun findWithMultiCurrency(lastUpdateAt: Instant): Flow<ShortItem> {
        val query = Query(
            Criteria().andOperator(
                LegacyShortItem::multiCurrency isEqualTo true,
                LegacyShortItem::lastUpdatedAt lte lastUpdateAt
            )
        ).withHint(MULTI_CURRENCY_DEFINITION.indexKeys)

        return template.find(query, LegacyShortItem::class.java)
            .asFlow()
            .map { it.toShortItem() }
    }

    override fun findByBlockchain(
        fromShortItemId: ShortItemId?, blockchain: BlockchainDto?, limit: Int
    ): Flow<ShortItem> {
        val legacyFromId = fromShortItemId?.let { LegacyShortItemId(it) }
        val criteriaList = listOfNotNull(
            blockchain?.let { LegacyShortItem::blockchain isEqualTo it },
            legacyFromId?.let { Criteria.where("_id").gt(it) }
        )

        val criteria = if (criteriaList.isEmpty()) Criteria() else Criteria().andOperator(criteriaList)

        val query = Query(criteria)
            .with(Sort.by("_id"))
            .limit(limit)

        return template.find(query, LegacyShortItem::class.java)
            .asFlow()
            .map { it.toShortItem() }
    }

    override fun findByAuction(auctionId: AuctionIdDto): Flow<ShortItem> {
        val query = Query(LegacyShortItem::auctions isEqualTo auctionId)
        return template.find(query, LegacyShortItem::class.java)
            .asFlow()
            .map { it.toShortItem() }
    }

    override fun findByPlatformWithSell(platform: PlatformDto, fromShortItemId: ShortItemId?): Flow<ShortItem> {
        val legacyFromId = fromShortItemId?.let { LegacyShortItemId(it) }
        val criteria = Criteria().andOperator(
            listOfNotNull(
                LegacyShortItem::bestSellOrder / ShortOrder::platform isEqualTo platform.name,
                legacyFromId?.let { Criteria.where("_id").gt(it) }
            )
        )
        val query = Query(criteria)
            .with(Sort.by("${LegacyShortItem::bestSellOrder.name}.${ShortOrder::platform.name}", "_id"))
            .withHint(BY_BEST_SELL_PLATFORM_DEFINITION.indexKeys)

        return template.find(query, LegacyShortItem::class.java)
            .asFlow()
            .map { it.toShortItem() }
    }

    override suspend fun delete(itemId: ShortItemId): DeleteResult? {
        val legacyId = LegacyShortItemId(itemId)
        val criteria = Criteria("_id").isEqualTo(legacyId)
        return template.remove(Query(criteria), collection).awaitFirstOrNull()
    }

    companion object {

        private val BLOCKCHAIN_DEFINITION = Index()
            .on(LegacyShortItem::blockchain.name, Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .background()

        private val MULTI_CURRENCY_DEFINITION = Index()
            .on(LegacyShortItem::multiCurrency.name, Sort.Direction.DESC)
            .on(LegacyShortItem::lastUpdatedAt.name, Sort.Direction.DESC)
            .background()

        private val BY_BEST_SELL_PLATFORM_DEFINITION = Index()
            .on("${LegacyShortItem::bestSellOrder.name}.${ShortOrder::platform.name}", Sort.Direction.ASC)
            .on("_id", Sort.Direction.ASC)
            .background()

        private val AUCTION_DEFINITION = Index()
            .on(LegacyShortItem::auctions.name, Sort.Direction.DESC)
            .background()

        private val ALL_INDEXES = listOf(
            BLOCKCHAIN_DEFINITION,
            MULTI_CURRENCY_DEFINITION,
            BY_BEST_SELL_PLATFORM_DEFINITION,
            AUCTION_DEFINITION
        )
    }
}
