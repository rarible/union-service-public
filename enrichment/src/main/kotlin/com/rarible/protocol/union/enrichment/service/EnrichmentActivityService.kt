package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipSourceDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.enrichment.converter.ActivityDtoConverter
import com.rarible.protocol.union.enrichment.converter.EnrichmentActivityConverter
import com.rarible.protocol.union.enrichment.converter.ItemLastSaleConverter
import com.rarible.protocol.union.enrichment.converter.data.EnrichmentActivityData
import com.rarible.protocol.union.enrichment.custom.collection.CustomCollectionResolutionRequest
import com.rarible.protocol.union.enrichment.custom.collection.CustomCollectionResolver
import com.rarible.protocol.union.enrichment.model.EnrichmentActivity
import com.rarible.protocol.union.enrichment.model.ItemLastSale
import com.rarible.protocol.union.enrichment.repository.ActivityRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EnrichmentActivityService(
    private val customCollectionResolver: CustomCollectionResolver,
    private val activityRepository: ActivityRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun update(activity: UnionActivity): EnrichmentActivity {
        val enrichmentActivity = enrich(activity)
        if (activity.reverted == true) {
            activityRepository.delete(enrichmentActivity.id)
        } else {
            activityRepository.save(enrichmentActivity)
        }
        return enrichmentActivity
    }

    @Deprecated("remove after enabling ff.enableMongoActivityWrite")
    suspend fun enrichDeprecated(activities: List<UnionActivity>): List<ActivityDto> {
        val request = activities.map { CustomCollectionResolutionRequest(it.id, it.itemId(), it.collectionId()) }
        val customCollections = customCollectionResolver.resolve(request, emptyMap())
        val data = EnrichmentActivityData(customCollections)
        return activities.map { ActivityDtoConverter.convert(it, data) }
    }

    suspend fun enrich(activity: UnionActivity): EnrichmentActivity {
        return enrich(listOf(activity)).first()
    }

    suspend fun enrich(activities: List<UnionActivity>): List<EnrichmentActivity> {
        val request = activities.map { CustomCollectionResolutionRequest(it.id, it.itemId(), it.collectionId()) }
        val customCollections = customCollectionResolver.resolve(request, emptyMap())
        val data = EnrichmentActivityData(customCollections)
        return activities.map { EnrichmentActivityConverter.convert(it, data) }
    }

    suspend fun getOwnershipSource(ownershipId: OwnershipIdDto): OwnershipSourceDto {
        val itemId = ownershipId.getItemId()

        if (isItemMint(itemId, ownershipId.owner)) return OwnershipSourceDto.MINT

        if (isItemPurchase(itemId, ownershipId.owner)) return OwnershipSourceDto.PURCHASE

        return OwnershipSourceDto.TRANSFER
    }

    private suspend fun isItemMint(itemId: ItemIdDto, owner: UnionAddress): Boolean {
        return activityRepository.isMinter(itemId, owner)
    }

    private suspend fun isItemPurchase(itemId: ItemIdDto, owner: UnionAddress): Boolean {
        return activityRepository.isBuyer(itemId, owner)
    }

    suspend fun getItemLastSale(itemId: ItemIdDto): ItemLastSale? {
        val sell = activityRepository.findLastSale(itemId)
        val result = ItemLastSaleConverter.convert(sell)

        if (result == null) {
            logger.info("Last sale NOT found for Item [{}]", itemId)
        } else {
            logger.info("Last sale found for Item [{}] : activity = [{}], lastSale = {}", itemId, sell?.id, result)
        }

        return result
    }
}
