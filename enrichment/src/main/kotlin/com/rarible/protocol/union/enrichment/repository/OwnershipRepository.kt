package com.rarible.protocol.union.enrichment.repository

import com.mongodb.client.result.DeleteResult
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.model.*
import kotlinx.coroutines.flow.Flow
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
import org.springframework.data.mongodb.core.query.*
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.time.Instant

@Component
class OwnershipRepository(
    private val template: ReactiveMongoTemplate
) {

    private val logger = LoggerFactory.getLogger(OwnershipRepository::class.java)

    val collection: String = template.getCollectionName(ShortOwnership::class.java)

    suspend fun save(ownership: ShortOwnership): ShortOwnership {
        return template.save(ownership).awaitFirst()
    }

    suspend fun get(id: ShortOwnershipId): ShortOwnership? {
        return template.findById<ShortOwnership>(id).awaitFirstOrNull()
    }

    suspend fun findAll(ids: List<ShortOwnershipId>): List<ShortOwnership> {
        return template.find<ShortOwnership>(
            Query(ShortOwnership::id inValues ids),
            ShortOwnership::class.java
        ).collectList().awaitFirst()
    }

    fun findWithMultiCurrency(lastUpdateAt: Instant): Flow<ShortOwnershipId> {
        val criteria = Criteria().orOperator(
            ShortOwnership::bestSellOrderCount gt 1
        ).and(ShortOwnership::lastUpdatedAt).lte(lastUpdateAt)

        val blockchainField = ShortOwnership::blockchain.name
        val tokenField = ShortOwnership::token.name
        val tokenIdField = ShortOwnership::tokenId.name
        val ownerField = ShortOwnership::owner.name

        val query = Query(criteria).withHint(Indices.MULTI_CURRENCY_OWNERSHIP.indexKeys)

        query.fields()
            .include(blockchainField)
            .include(tokenField)
            .include(tokenIdField)
            .include(ownerField)

        return template.find(query, Document::class.java, collection)
            .map { document ->
                ShortOwnershipId(
                    blockchain = BlockchainDto.valueOf(document.getString(blockchainField)),
                    token = document.getString(tokenField),
                    tokenId = BigInteger(document.getString(tokenIdField)),
                    owner = document.getString(ownerField)
                )
            }
            .asFlow()
    }

    suspend fun delete(ownershipId: ShortOwnershipId): DeleteResult? {
        val criteria = Criteria("_id").isEqualTo(ownershipId)
        return template.remove(Query(criteria), collection).awaitFirstOrNull()
    }

    suspend fun getItemSellStats(itemId: ShortItemId): ItemSellStats {
        val bestSellOrderField = ShortOwnership::bestSellOrder.name
        val makeStockField = ShortOrder::makeStock.name
        val query = Query(
            Criteria().andOperator(
                ShortOwnership::blockchain isEqualTo itemId.blockchain,
                ShortOwnership::token isEqualTo itemId.token,
                ShortOwnership::tokenId isEqualTo itemId.tokenId,
                ShortOwnership::bestSellOrder exists true
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

    suspend fun createIndices() = runBlocking {
        Indices.ALL.forEach { index ->
            logger.info("Ensure index '{}' for collection '{}'", index, collection)
            template.indexOps(collection).ensureIndex(index).awaitFirst()
        }
    }

    object Indices {

        private val OWNERSHIP_CONTRACT_TOKENID: Index = Index()
            .on(ShortOwnership::blockchain.name, Sort.Direction.ASC)
            .on(ShortOwnership::token.name, Sort.Direction.ASC)
            .on(ShortOwnership::tokenId.name, Sort.Direction.ASC)
            .background()

        val MULTI_CURRENCY_OWNERSHIP: Index = Index()
            .on(ShortOwnership::bestSellOrderCount.name, Sort.Direction.DESC)
            .on(ShortOwnership::blockchain.name, Sort.Direction.DESC)
            .on(ShortOwnership::token.name, Sort.Direction.DESC)
            .on(ShortOwnership::tokenId.name, Sort.Direction.DESC)
            .on(ShortOwnership::owner.name, Sort.Direction.DESC)
            .on(ShortOwnership::lastUpdatedAt.name, Sort.Direction.DESC)
            .background()

        val ALL = listOf(
            OWNERSHIP_CONTRACT_TOKENID,
            MULTI_CURRENCY_OWNERSHIP
        )
    }
}
