package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.model.UnionMintActivity
import com.rarible.protocol.union.core.model.UnionTransferActivity
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
import com.rarible.protocol.union.enrichment.converter.EnrichmentActivityConverter
import com.rarible.protocol.union.enrichment.converter.ItemLastSaleConverter
import com.rarible.protocol.union.enrichment.converter.data.EnrichmentActivityData
import com.rarible.protocol.union.enrichment.model.EnrichmentActivity
import com.rarible.protocol.union.enrichment.model.ItemLastSale
import com.rarible.protocol.union.enrichment.repository.ActivityRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EnrichmentActivityService(
    private val activityRouter: BlockchainRouter<ActivityService>,
    private val customCollectionResolver: CustomCollectionResolver,
    private val activityRepository: ActivityRepository,
    private val featureFlagsProperties: FeatureFlagsProperties,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Deprecated("remove after enabling ff.enableMongoActivityWrite")
    suspend fun enrichDeprecated(activities: List<UnionActivity>): List<ActivityDto> {
        return activities.map { enrichDeprecated(it) }
    }

    @Deprecated("remove after enabling ff.enableMongoActivityWrite")
    suspend fun enrichDeprecated(activity: UnionActivity): ActivityDto {
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

    suspend fun enrich(activity: UnionActivity): EnrichmentActivity {
        // We expect here only one of them != null
        val itemId = activity.itemId()
        val collectionId = activity.collectionId()

        val customCollection = when {
            itemId != null -> customCollectionResolver.resolveCustomCollection(itemId)
            collectionId != null -> customCollectionResolver.resolveCustomCollection(collectionId)
            else -> null
        }

        val data = EnrichmentActivityData(customCollection)
        return EnrichmentActivityConverter.convert(activity, data)
    }

    suspend fun enrich(activities: List<UnionActivity>): List<EnrichmentActivity> {
        return activities.map { enrich(it) }
    }

    suspend fun getOwnershipSource(ownershipId: OwnershipIdDto): OwnershipSourceDto {
        val itemId = ownershipId.getItemId()

        if (isItemMint(itemId, ownershipId.owner)) return OwnershipSourceDto.MINT

        if (isItemPurchase(itemId, ownershipId.owner)) return OwnershipSourceDto.PURCHASE

        return OwnershipSourceDto.TRANSFER
    }

    private suspend fun isItemMint(itemId: ItemIdDto, owner: UnionAddress): Boolean {
        if (featureFlagsProperties.enableMongoActivityRead) {
            return activityRepository.isMinter(itemId, owner)
        }
        // For item there should be only one mint
        val mint = activityRouter.getService(itemId.blockchain).getActivitiesByItem(
            types = listOf(ActivityTypeDto.MINT),
            itemId = itemId.value,
            continuation = null,
            size = 1,
            sort = ActivitySortDto.LATEST_FIRST
        ).entities.firstOrNull()

        // Originally, there ALWAYS should be a mint
        if (mint == null || (mint as UnionMintActivity).owner != owner) {
            logger.info("Mint activity NOT found for Item [{}] and owner [{}]", itemId, owner.fullId())
            return false
        }

        logger.info("Mint Activity found for Item [{}] and owner [{}]: [{}]", itemId, owner.fullId(), mint.id)
        return true
    }

    private suspend fun isItemPurchase(itemId: ItemIdDto, owner: UnionAddress): Boolean {
        if (featureFlagsProperties.enableMongoActivityRead) {
            return activityRepository.isBuyer(itemId, owner)
        }
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
                (it is UnionTransferActivity)
                    && it.owner == owner
                    && it.purchase == true
            }
            if (purchase != null) {
                logger.info(
                    "Transfer (purchase) Activity found for Item [{}] and owner [{}]: [{}]",
                    itemId, owner.fullId(), purchase.id
                )
                return true
            }
            continuation = response.continuation
        } while (continuation != null)

        logger.info("Transfer (purchase) activity NOT found for Item [{}] and owner [{}]", itemId, owner.fullId())
        return false
    }

    suspend fun getItemLastSale(itemId: ItemIdDto): ItemLastSale? =
        if (featureFlagsProperties.enableMongoActivityRead) {
            val sell = activityRepository.findLastSale(itemId)
            val result = ItemLastSaleConverter.convert(sell)

            if (result == null) {
                logger.info("Last sale NOT found for Item [{}]", itemId)
            } else {
                logger.info("Last sale found for Item [{}] : activity = [{}], lastSale = {}", itemId, sell?.id, result)
            }

            result
        } else {
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
            result
        }
}