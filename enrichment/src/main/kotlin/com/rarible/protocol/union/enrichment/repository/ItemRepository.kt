package com.rarible.protocol.union.enrichment.repository

import com.mongodb.client.result.DeleteResult
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.model.ShortItem
import com.rarible.protocol.union.enrichment.model.ShortItemId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.bson.Document
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.findById
import org.springframework.data.mongodb.core.index.Index
import org.springframework.data.mongodb.core.query.*
import org.springframework.stereotype.Component
import java.math.BigInteger
import java.time.Instant

@Component
class ItemRepository(
    private val template: ReactiveMongoTemplate
) {
    suspend fun createIndexes()  {
        ALL_INDEXES.forEach { index ->
            template.indexOps(COLLECTION).ensureIndex(index).awaitFirst()
        }
    }

    suspend fun save(item: ShortItem): ShortItem {
        return template.save(item, COLLECTION).awaitFirst()
    }

    suspend fun get(id: ShortItemId): ShortItem? {
        return template.findById<ShortItem>(id, COLLECTION).awaitFirstOrNull()
    }

    suspend fun findAll(ids: List<ShortItemId>): List<ShortItem> {
        val criteria = Criteria("_id").inValues(ids)
        return template.find<ShortItem>(Query(criteria), COLLECTION).collectList().awaitFirst()
    }

    fun findWithMultiCurrency(lastUpdateAt: Instant): Flow<ShortItemId> {
        val criteria = Criteria().orOperator(
            ShortItem::bestSellOrderCount gt 1,
            ShortItem::bestBidOrderCount gt 1
        ).and(ShortItem::lastUpdatedAt).lte(lastUpdateAt)

        val blockchainField = ShortItem::blockchain.name
        val tokenField = ShortItem::token.name
        val tokenIdField = ShortItem::tokenId.name

        val query = Query(criteria).withHint(MULTI_CURRENCY_DEFINITION.indexKeys)

        query.fields()
            .include(blockchainField)
            .include(tokenField)
            .include(tokenIdField)

        return template.find(query, Document::class.java, COLLECTION)
            .map { document ->
                ShortItemId(
                    blockchain = BlockchainDto.valueOf(document.getString(blockchainField)),
                    token = document.getString(tokenField),
                    tokenId = BigInteger(document.getString(tokenIdField))
                )
            }
            .asFlow()
    }

    suspend fun delete(itemId: ShortItemId): DeleteResult? {
        val criteria = Criteria("_id").isEqualTo(itemId)
        return template.remove(Query(criteria), COLLECTION).awaitFirstOrNull()
    }

    companion object {
        private val MULTI_CURRENCY_DEFINITION = Index()
            .on(ShortItem::bestSellOrderCount.name, Sort.Direction.DESC)
            .on(ShortItem::bestBidOrderCount.name, Sort.Direction.DESC)
            .on(ShortItem::blockchain.name, Sort.Direction.DESC)
            .on(ShortItem::token.name, Sort.Direction.DESC)
            .on(ShortItem::tokenId.name, Sort.Direction.DESC)
            .on(ShortItem::lastUpdatedAt.name, Sort.Direction.DESC)
            .background()

        private val ALL_INDEXES = listOf(
            MULTI_CURRENCY_DEFINITION
        )

        const val COLLECTION = "item"
    }
}
