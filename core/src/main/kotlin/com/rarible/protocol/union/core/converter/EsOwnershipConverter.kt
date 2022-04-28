package com.rarible.protocol.union.core.converter

import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.model.getItemId
import com.rarible.protocol.union.core.model.getSellerOwnershipId
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.OwnershipDto

object EsOwnershipConverter {

    fun convert(source: OwnershipDto): EsOwnership = EsOwnership(
        ownershipId = source.id.fullId(),
        blockchain = source.blockchain,
        itemId = source.itemId?.fullId(),
        collection = source.collection?.fullId(),
        owner = source.owner.fullId(),
        date = source.createdAt,
        auctionId = source.auction?.id?.fullId(),
    )

    fun convert(source: UnionOwnership): EsOwnership = EsOwnership(
        ownershipId = source.id.fullId(),
        blockchain = source.id.blockchain,
        itemId = source.id.getItemId().fullId(),
        collection = source.collection?.fullId(),
        owner = source.id.owner.fullId(),
        date = source.createdAt,
        auctionId = null,
    )

    fun convert(source: AuctionDto) = EsOwnership(
        ownershipId = source.getSellerOwnershipId().fullId(),
        blockchain = source.id.blockchain,
        itemId = source.getItemId().fullId(),
        collection = null,
        owner = source.seller.fullId(),
        date = source.createdAt,
        auctionId = source.id.fullId(),
    )

    fun convertOrMerge(source: UnionOwnership, esOwnership: EsOwnership?): EsOwnership = when (esOwnership) {
        null -> convert(source)
        else -> esOwnership.copy(
            collection = source.collection?.fullId(),
        )
    }

    fun convertOrMerge(source: AuctionDto, esOwnership: EsOwnership?): EsOwnership = when (esOwnership) {
        null -> convert(source)
        else -> esOwnership.copy(
            auctionId = source.id.fullId(),
        )
    }
}
