package com.rarible.protocol.union.enrichment.service

import com.mongodb.client.result.DeleteResult
import com.rarible.core.apm.SpanType
import com.rarible.core.apm.withSpan
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.enrichment.model.ShortCollection
import com.rarible.protocol.union.enrichment.model.ShortCollectionId
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component

@Component
class EnrichmentCollectionService(
    val collectionRepository: CollectionRepository,
    private val enrichmentOrderService: EnrichmentOrderService
) {

    suspend fun get(collectionId: ShortCollectionId): ShortCollection? {
        return collectionRepository.get(collectionId)
    }

    suspend fun save(collection: ShortCollection): ShortCollection? {
        return collectionRepository.save(collection.withCalculatedFields())
    }

    suspend fun getAll(): List<ShortCollection> {
        return collectionRepository.getAll()
    }

    suspend fun delete(collectionId: ShortCollectionId): DeleteResult? {
        return collectionRepository.delete(collectionId)
    }

    suspend fun enrichCollection(
        shortCollection: ShortCollection?,
        collectionDto: CollectionDto,
        orders: Map<OrderIdDto, OrderDto> = emptyMap(),
        loadMetaSynchronously: Boolean = false
    ) = coroutineScope {
        val bestSellOrder = withSpanAsync("fetchBestSellOrder", spanType = SpanType.EXT) {
            enrichmentOrderService.fetchOrderIfDiffers(shortCollection?.bestSellOrder, orders)
        }
        val bestBidOrder = withSpanAsync("fetchBestBidOrder", spanType = SpanType.EXT) {
            enrichmentOrderService.fetchOrderIfDiffers(shortCollection?.bestBidOrder, orders)
        }

        val bestOrders = listOf(bestSellOrder, bestBidOrder)
            .awaitAll().filterNotNull()
            .associateBy { it.id }

        val updatedCollectionDto = collectionDto.copy(
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