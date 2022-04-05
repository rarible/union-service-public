package com.rarible.protocol.union.enrichment.service

import com.mongodb.client.result.DeleteResult
import com.rarible.core.apm.SpanType
import com.rarible.core.apm.withSpan
import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.enrichment.model.ShortCollection
import com.rarible.protocol.union.enrichment.model.ShortCollectionId
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.util.spent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EnrichmentCollectionService(
    private val collectionServiceRouter: BlockchainRouter<CollectionService>,
    private val collectionRepository: CollectionRepository,
    private val enrichmentOrderService: EnrichmentOrderService
) {
    private val logger = LoggerFactory.getLogger(EnrichmentCollectionService::class.java)

    suspend fun get(collectionId: ShortCollectionId): ShortCollection? {
        return collectionRepository.get(collectionId)
    }

    suspend fun save(collection: ShortCollection): ShortCollection? {
        return collectionRepository.save(collection.withCalculatedFields())
    }

    suspend fun getOrEmpty(collectionId: ShortCollectionId): ShortCollection {
        return collectionRepository.get(collectionId) ?: ShortCollection.empty(collectionId)
    }

    suspend fun getAll(): List<ShortCollection> {
        return collectionRepository.getAll()
    }

    suspend fun delete(collectionId: ShortCollectionId): DeleteResult? {
        return collectionRepository.delete(collectionId)
    }

    suspend fun fetch(collectionId: ShortCollectionId): CollectionDto {
        val now = nowMillis()
        val itemDto = collectionServiceRouter.getService(collectionId.blockchain).getCollectionById(collectionId.collectionId)
        logger.info("Fetched collection [{}] ({} ms)", collectionId.toDto().fullId(), spent(now))
        return itemDto
    }

    suspend fun enrichCollection(
        shortCollection: ShortCollection?,
        collectionDto: CollectionDto?,
        orders: Map<OrderIdDto, OrderDto> = emptyMap(),
        loadMetaSynchronously: Boolean = false
    ) = coroutineScope {
        require(shortCollection != null || collectionDto != null)
        val collectionId = shortCollection?.id?.toDto() ?: collectionDto!!.id
        val fetchedItem = withSpanAsync("fetchItem", spanType = SpanType.EXT) {
            collectionDto ?: fetch(ShortCollectionId(collectionId))
        }

        val bestSellOrder = withSpanAsync("fetchBestSellOrder", spanType = SpanType.EXT) {
            enrichmentOrderService.fetchOrderIfDiffers(shortCollection?.bestSellOrder, orders)
        }
        val bestBidOrder = withSpanAsync("fetchBestBidOrder", spanType = SpanType.EXT) {
            enrichmentOrderService.fetchOrderIfDiffers(shortCollection?.bestBidOrder, orders)
        }

        val bestOrders = listOf(bestSellOrder, bestBidOrder)
            .awaitAll().filterNotNull()
            .associateBy { it.id }

        val updatedCollectionDto = fetchedItem.await().copy(
            bestBidOrder = shortCollection?.bestBidOrder?.let { bestOrders[it.dtoId] },
            bestSellOrder = shortCollection?.bestSellOrder?.let {bestOrders[it.dtoId]}
        )
        updatedCollectionDto
    }

    private fun <T> CoroutineScope.withSpanAsync(
        spanName: String,
        spanType: String = SpanType.APP,
        block: suspend () -> T
    ): Deferred<T> = async { withSpan(name = spanName, type = spanType, body = block) }

}