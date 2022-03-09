package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipSourceDto
import com.rarible.protocol.union.dto.TransferActivityDto
import com.rarible.protocol.union.dto.UnionAddress
import org.springframework.stereotype.Component

@Component
class EnrichmentActivityService(
    private val activityRouter: BlockchainRouter<ActivityService>
) {

    suspend fun getOwnershipSource(ownershipId: OwnershipIdDto): OwnershipSourceDto {
        val itemId = ownershipId.getItemId()

        val mint = getItemMint(itemId, ownershipId.owner)
        if (mint != null) return OwnershipSourceDto.MINT

        val purchase = getItemPurchase(itemId, ownershipId.owner)
        if (purchase != null) return OwnershipSourceDto.PURCHASE

        return OwnershipSourceDto.TRANSFER
    }

    private suspend fun getItemMint(itemId: ItemIdDto, owner: UnionAddress): ActivityDto? {
        // For item there should be only one mint
        val mint = activityRouter.getService(itemId.blockchain).getActivitiesByItem(
            types = listOf(ActivityTypeDto.MINT),
            itemId = itemId.value,
            continuation = null,
            size = 1,
            sort = ActivitySortDto.LATEST_FIRST
        ).entities.firstOrNull()

        // Originally, there ALWAYS should be a mint
        if (mint == null || (mint as MintActivityDto).owner != owner) {
            return null
        }

        return mint
    }

    private suspend fun getItemPurchase(itemId: ItemIdDto, owner: UnionAddress): ActivityDto? {
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
                (it is TransferActivityDto)
                    && it.owner == owner
                    && it.purchase == true
            }
            if (purchase != null) {
                return purchase
            }
            continuation = response.continuation
        } while (continuation != null)

        return null
    }

}