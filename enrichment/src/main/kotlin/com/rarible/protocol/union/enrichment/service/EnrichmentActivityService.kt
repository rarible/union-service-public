package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.model.UnionActivityDto
import com.rarible.protocol.union.core.model.UnionMintActivityDto
import com.rarible.protocol.union.core.model.UnionTransferActivityDto
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipSourceDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.enrichment.converter.ActivityDtoConverter
import com.rarible.protocol.union.enrichment.converter.ItemLastSaleConverter
import com.rarible.protocol.union.enrichment.converter.data.EnrichmentActivityData
import com.rarible.protocol.union.enrichment.model.ItemLastSale
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EnrichmentActivityService(
    private val activityRouter: BlockchainRouter<ActivityService>,
    private val customCollectionResolver: CustomCollectionResolver
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun enrich(activity: UnionActivityDto): ActivityDto {
        // We expect here only one of them != null
        val itemId = activity.itemId()
        val collectionId = activity.collectionId()

        val customCollection = when {
            itemId != null -> customCollectionResolver.resolveCustomCollection(itemId)
            collectionId != null -> customCollectionResolver.resolveCustomCollection(collectionId)
            else -> null
        }

        val data = EnrichmentActivityData(customCollection)
        return ActivityDtoConverter.convert(activity, data)
    }

    suspend fun enrich(activities: List<UnionActivityDto>): List<ActivityDto> {
        return activities.map { enrich(it) }
    }

    suspend fun getOwnershipSource(ownershipId: OwnershipIdDto): OwnershipSourceDto {
        val itemId = ownershipId.getItemId()

        val mint = getItemMint(itemId, ownershipId.owner)
        if (mint != null) return OwnershipSourceDto.MINT

        val purchase = getItemPurchase(itemId, ownershipId.owner)
        if (purchase != null) return OwnershipSourceDto.PURCHASE

        return OwnershipSourceDto.TRANSFER
    }

    private suspend fun getItemMint(itemId: ItemIdDto, owner: UnionAddress): UnionActivityDto? {
        // For item there should be only one mint
        val mint = activityRouter.getService(itemId.blockchain).getActivitiesByItem(
            types = listOf(ActivityTypeDto.MINT),
            itemId = itemId.value,
            continuation = null,
            size = 1,
            sort = ActivitySortDto.LATEST_FIRST
        ).entities.firstOrNull()

        // Originally, there ALWAYS should be a mint
        if (mint == null || (mint as UnionMintActivityDto).owner != owner) {
            logger.info("Mint activity NOT found for Item [{}] and owner [{}]", itemId, owner.fullId())
            return null
        }

        logger.info("Mint Activity found for Item [{}] and owner [{}]: [{}]", itemId, owner.fullId(), mint.id)
        return mint
    }

    private suspend fun getItemPurchase(itemId: ItemIdDto, owner: UnionAddress): UnionActivityDto? {
        // TODO not sure this is a good way to search transfer, ideally there should be filter by user
        var continuation: String? = null
        do {
            val response = activityRouter.getService(itemId.blockchain).getActivitiesByItem(
                types = listOf(ActivityTypeDto.TRANSFER),
                itemId = itemId.value,
                continuation = continuation,
                size = 100,
                sort = ActivitySortDto.LATEST_FIRST
            )
            val purchase = response.entities.firstOrNull {
                (it is UnionTransferActivityDto)
                    && it.owner == owner
                    && it.purchase == true
            }
            if (purchase != null) {
                logger.info(
                    "Transfer (purchase) Activity found for Item [{}] and owner [{}]: [{}]",
                    itemId, owner.fullId(), purchase.id
                )
                return purchase
            }
            continuation = response.continuation
        } while (continuation != null)

        logger.info("Transfer (purchase) activity NOT found for Item [{}] and owner [{}]", itemId, owner.fullId())
        return null
    }

    suspend fun getItemLastSale(itemId: ItemIdDto): ItemLastSale? {
        val sell = activityRouter.getService(itemId.blockchain).getActivitiesByItem(
            types = listOf(ActivityTypeDto.SELL), // TODO what about auctions and on-chain orders?
            itemId = itemId.value,
            continuation = null,
            size = 1,
            sort = ActivitySortDto.LATEST_FIRST
        ).entities.firstOrNull()

        val result = ItemLastSaleConverter.convert(sell)

        if (result == null) {
            logger.info("Last sale NOT found for Item [{}]", itemId)
        } else {
            logger.info("Last sale found for Item [{}] : activity = [{}], lastSale = {}", itemId, sell?.id, result)
        }

        return result
    }

}