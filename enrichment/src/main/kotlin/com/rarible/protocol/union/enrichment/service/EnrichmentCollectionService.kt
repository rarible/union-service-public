package com.rarible.protocol.union.enrichment.service

import com.mongodb.client.result.DeleteResult
import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.enrichment.converter.EnrichmentCollectionConverter
import com.rarible.protocol.union.enrichment.model.ShortCollection
import com.rarible.protocol.union.enrichment.model.ShortCollectionId
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.util.spent
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

    suspend fun findAll(ids: List<ShortCollectionId>): List<ShortCollection> {
        return collectionRepository.getAll(ids)
    }

    suspend fun delete(collectionId: ShortCollectionId): DeleteResult? {
        return collectionRepository.delete(collectionId)
    }

    suspend fun fetch(collectionId: ShortCollectionId): UnionCollection {
        val now = nowMillis()
        val collectionDto = collectionServiceRouter.getService(collectionId.blockchain).getCollectionById(collectionId.collectionId)
        logger.info("Fetched collection [{}] ({} ms)", collectionId.toDto().fullId(), spent(now))
        return collectionDto
    }

    suspend fun enrichCollection(
        shortCollection: ShortCollection?,
        collection: UnionCollection?,
        orders: Map<OrderIdDto, OrderDto> = emptyMap(),
        loadMetaSynchronously: Boolean = false
    ) = coroutineScope {
        require(shortCollection != null || collection != null)
        val collectionId = shortCollection?.id?.toDto() ?: collection!!.id
        val fetchedCollection = async {
            collection ?: fetch(ShortCollectionId(collectionId))
        }

        val bestSellOrder = async {
            enrichmentOrderService.fetchOrderIfDiffers(shortCollection?.bestSellOrder, orders)
        }
        val bestBidOrder = async {
            enrichmentOrderService.fetchOrderIfDiffers(shortCollection?.bestBidOrder, orders)
        }

        val bestOrders = listOf(bestSellOrder, bestBidOrder)
            .awaitAll().filterNotNull()
            .associateBy { it.id }

        val collectionDto = EnrichmentCollectionConverter.convert(
            collection = fetchedCollection.await(),
            shortCollection = shortCollection,
            orders = bestOrders
        )
        logger.info("Enriched collection {}: {}", collectionId.fullId(), collectionDto)
        collectionDto
    }

}