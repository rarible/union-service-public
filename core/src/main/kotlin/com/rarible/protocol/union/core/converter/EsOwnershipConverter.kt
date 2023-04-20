package com.rarible.protocol.union.core.converter

import com.rarible.protocol.union.core.converter.helper.getCurrencyAddressOrNull
import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.core.model.getAuctionOwnershipId
import com.rarible.protocol.union.core.model.getItemId
import com.rarible.protocol.union.core.model.getSellerOwnershipId
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.OwnershipDto
import org.apache.commons.codec.digest.DigestUtils

object EsOwnershipConverter {

    fun convert(source: OwnershipDto): EsOwnership {
        val (id, original) = prepareOwnershipId(source.id.fullId())
        return EsOwnership(
            ownershipId = id,
            originalOwnershipId = original,
            blockchain = source.blockchain,
            itemId = source.itemId?.fullId(),
            collection = source.collection?.fullId(),
            owner = source.owner.fullId(),
            date = source.createdAt,
            auctionId = source.auction?.id?.fullId(),
            auctionOwnershipId = source.auction?.getAuctionOwnershipId()?.fullId(),
            bestSellAmount = source.bestSellOrder?.take?.value?.toDouble(),
            bestSellCurrency = getCurrencyAddressOrNull(source.blockchain, source.bestSellOrder?.take),
            bestSellMarketplace = source.bestSellOrder?.platform?.name, // getting marketplace may be more complicated
        )
    }

    @Deprecated("Use convert(OwnershipDto) instead")
    fun convert(source: AuctionDto): EsOwnership {
        val (id, original) = prepareOwnershipId(source.getSellerOwnershipId().fullId())
        return EsOwnership(
            ownershipId = id,
            originalOwnershipId = original,
            blockchain = source.id.blockchain,
            itemId = source.getItemId().fullId(),
            collection = null,
            owner = source.seller.fullId(),
            date = source.createdAt,
            auctionId = source.id.fullId(),
            auctionOwnershipId = source.getAuctionOwnershipId().fullId(),
        )
    }

    // Returns a pair of ownershipId and originalOwnershipId
    private fun prepareOwnershipId(id: String): Pair<String, String?> {
        if (id.encodeToByteArray().size > 512) {
            return DigestUtils.sha256Hex(id) to id
        }
        return id to null
    }
}
