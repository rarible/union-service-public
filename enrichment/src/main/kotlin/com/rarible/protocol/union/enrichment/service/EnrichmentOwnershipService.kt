package com.rarible.protocol.union.enrichment.service

import com.mongodb.client.result.DeleteResult
import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.UnionOwnershipDto
import com.rarible.protocol.union.enrichment.converter.EnrichedOwnershipConverter
import com.rarible.protocol.union.enrichment.model.ItemSellStats
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.repository.OwnershipRepository
import com.rarible.protocol.union.enrichment.util.spent
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EnrichmentOwnershipService(
    private val ownershipServiceRouter: BlockchainRouter<OwnershipService>,
    private val ownershipRepository: OwnershipRepository,
    private val enrichmentOrderService: EnrichmentOrderService
) {

    private val logger = LoggerFactory.getLogger(EnrichmentOwnershipService::class.java)

    suspend fun get(ownershipId: ShortOwnershipId): ShortOwnership? {
        return ownershipRepository.get(ownershipId)
    }

    suspend fun getOrEmpty(ownershipId: ShortOwnershipId): ShortOwnership {
        return ownershipRepository.get(ownershipId) ?: ShortOwnership.empty(ownershipId)
    }

    suspend fun save(ownership: ShortOwnership): ShortOwnership {
        return ownershipRepository.save(ownership.withCalculateFields())
    }

    suspend fun delete(ownershipId: ShortOwnershipId): DeleteResult? {
        val result = ownershipRepository.delete(ownershipId)
        logger.debug("Deleted Ownership [{}], deleted: {}", ownershipId, result?.deletedCount)
        return result
    }

    suspend fun findAll(ids: List<ShortOwnershipId>): List<ShortOwnership> {
        return ownershipRepository.findAll(ids)
    }

    suspend fun getItemSellStats(itemId: ShortItemId): ItemSellStats {
        val now = nowMillis()
        val result = ownershipRepository.getItemSellStats(itemId)
        logger.info("SellStat query executed for ItemId [{}]: [{}] ({}ms)", itemId, result, spent(now))
        return result
    }

    suspend fun fetch(ownershipId: ShortOwnershipId): UnionOwnershipDto {
        val now = nowMillis()
        val ownershipDto = ownershipServiceRouter.getService(ownershipId.blockchain)
            .getOwnershipById(ownershipId.toDto().value)

        logger.info("Fetched Ownership by Id [{}] ({}ms)", ownershipId, spent(now))
        return ownershipDto
    }

    suspend fun fetchAllByItemId(itemId: ShortItemId): List<UnionOwnershipDto> {
        var continuation: String? = null
        val result = ArrayList<UnionOwnershipDto>()
        do {
            val page = ownershipServiceRouter.getService(itemId.blockchain).getOwnershipsByItem(
                itemId.token,
                itemId.tokenId.toString(),
                continuation,
                1000 // TODO
            )
            result.addAll(page.entities)
            continuation = page.continuation
        } while (continuation != null)
        return result
    }

    suspend fun enrichOwnership(
        short: ShortOwnership,
        ownership: UnionOwnershipDto? = null,
        order: OrderDto? = null
    ) = coroutineScope {
        val fetchedOwnership = async { ownership ?: fetch(short.id) }
        val bestSellOrder = enrichmentOrderService.fetchOrderIfDiffers(short.bestSellOrder, order)

        val orders = listOfNotNull(bestSellOrder)
            .associateBy { it.id }

        EnrichedOwnershipConverter.convert(fetchedOwnership.await(), short, orders)
    }

}