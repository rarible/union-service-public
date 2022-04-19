package com.rarible.protocol.union.core.converter

import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.dto.OwnershipDto

object EsOwnershipConverter {
    fun convert(source: OwnershipDto): EsOwnership = EsOwnership(
        ownershipId = source.id.fullId(),
        blockchain = source.blockchain,
        token = source.contract?.fullId(),
        tokenId = source.tokenId?.toString(),
        itemId = source.itemId?.fullId(),
        collection = source.collection?.fullId(),
        owner = source.owner.fullId(),
        value = source.value.toString(),
        date = source.createdAt,
        price = source.bestSellOrder?.takePrice?.toString(),
        priceUsd = source.bestSellOrder?.takePriceUsd?.toString(),
        auctionEndDate = source.auction?.endTime,
        orderSource = source.bestSellOrder?.platform?.name,
    )
}
