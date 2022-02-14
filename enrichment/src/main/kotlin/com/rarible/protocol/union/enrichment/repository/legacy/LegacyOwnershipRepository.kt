package com.rarible.protocol.union.enrichment.repository.legacy

import com.mongodb.client.result.DeleteResult
import com.rarible.core.apm.CaptureSpan
import com.rarible.core.apm.SpanType
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.enrichment.model.ItemSellStats
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOrder
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.model.legacy.LegacyShortOwnership
import com.rarible.protocol.union.enrichment.model.legacy.LegacyShortOwnershipId
import com.rarible.protocol.union.enrichment.repository.OwnershipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrElse
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.bson.Document
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.exists
import org.springframework.data.mongodb.core.query.inValues
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.lte
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.time.Instant

@Component
@CaptureSpan(type = SpanType.DB)
@Deprecated("Should be replaced by implementation without token/tokenId")
class LegacyOwnershipRepository(
    private val template: ReactiveMongoTemplate
) : OwnershipRepository {

    private val logger = LoggerFactory.getLogger(LegacyOwnershipRepository::class.java)

    val collection: String = template.getCollectionName(LegacyShortOwnership::class.java)

    override suspend fun createIndices() = runBlocking {
        Indices.ALL.forEach { index ->
            logger.info("Ensure index '{}' for collection '{}'", index, collection)
            template.indexOps(collection).ensureIndex(index).awaitFirst()
        }
    }

    override suspend fun save(ownership: ShortOwnership): ShortOwnership {
        return template.save(LegacyShortOwnership(ownership)).awaitFirst().toShortOwnership()
    }

    override suspend fun get(id: ShortOwnershipId): ShortOwnership? {
        val legacyId = LegacyShortOwnershipId(id)
        return template.findById<LegacyShortOwnership>(legacyId)
            .awaitFirstOrNull()
            ?.toShortOwnership()
    }

    override suspend fun getAll(ids: List<ShortOwnershipId>): List<ShortOwnership> {
        val legacyIds = ids.map { LegacyShortOwnershipId(it) }
        return template.find(
            Query(LegacyShortOwnership::id inValues legacyIds),
            LegacyShortOwnership::class.java
        ).collectList()
            .awaitFirst()
            .map { it.toShortOwnership() }
    }

    fun findAll(fromShortOwnershipId: ShortOwnershipId?): Flow<ShortOwnership> {
        val legacyFromId = fromShortOwnershipId?.let { LegacyShortOwnershipId(it) }
        val criteria = Criteria().andOperator(
            listOfNotNull(
                legacyFromId?.let { Criteria.where("_id").gt(it) }
            )
        )

        val query = Query(criteria)
            .with(Sort.by("_id"))

        return template.find(query, LegacyShortOwnership::class.java)
            .asFlow()
            .map { it.toShortOwnership() }
    }

    override fun findWithMultiCurrency(lastUpdateAt: Instant): Flow<ShortOwnership> {
        val query = Query(
            Criteria().andOperator(
                LegacyShortOwnership::multiCurrency isEqualTo true,
                LegacyShortOwnership::lastUpdatedAt lte lastUpdateAt
            )
        ).withHint(Indices.MULTI_CURRENCY_OWNERSHIP.indexKeys)

        return template.find(query, LegacyShortOwnership::class.java)
            .asFlow()
            .map { it.toShortOwnership() }
    }

    override suspend fun delete(ownershipId: ShortOwnershipId): DeleteResult? {
        val legacyId = LegacyShortOwnershipId(ownershipId)
        val criteria = Criteria("_id").isEqualTo(legacyId)
        return template.remove(Query(criteria), collection).awaitFirstOrNull()
    }

    override suspend fun getItemSellStats(itemId: ShortItemId): ItemSellStats {
        val (token, tokenId) = CompositeItemIdParser.split(itemId.itemId)
        val bestSellOrderField = LegacyShortOwnership::bestSellOrder.name
        val makeStockField = ShortOrder::makeStock.name
        val query = Query(
            Criteria().andOperator(
                LegacyShortOwnership::blockchain isEqualTo itemId.blockchain,
                LegacyShortOwnership::token isEqualTo token,
                LegacyShortOwnership::tokenId isEqualTo tokenId,
                LegacyShortOwnership::bestSellOrder exists true
            )
        )

        query.fields().include("$bestSellOrderField.$makeStockField")

        // BigInteger stored as String, so we have to retrieve it and cast to Number manually
        val mapping = template.find(query, Document::class.java, collection)
            // each record means 1 unique ownership
            .map { 1 to BigInteger(it.get(bestSellOrderField, Document::class.java).getString(makeStockField)) }
            .reduce { n1, n2 -> Pair(n1.first + n2.first, n1.second.plus(n2.second)) }
            .awaitFirstOrElse { Pair(0, BigInteger.ZERO) }

        return ItemSellStats(mapping.first, mapping.second)
    }

    object Indices {

        private val OWNERSHIP_CONTRACT_TOKEN_ID: Index = Index()
            .on(LegacyShortOwnership::blockchain.name, Sort.Direction.ASC)
            .on(LegacyShortOwnership::token.name, Sort.Direction.ASC)
            .on(LegacyShortOwnership::tokenId.name, Sort.Direction.ASC)
            .background()

        val MULTI_CURRENCY_OWNERSHIP: Index = Index()
            .on(LegacyShortOwnership::multiCurrency.name, Sort.Direction.DESC)
            .on(LegacyShortOwnership::lastUpdatedAt.name, Sort.Direction.DESC)
            .background()

        val ALL = listOf(
            OWNERSHIP_CONTRACT_TOKEN_ID,
            MULTI_CURRENCY_OWNERSHIP
        )
    }
}
