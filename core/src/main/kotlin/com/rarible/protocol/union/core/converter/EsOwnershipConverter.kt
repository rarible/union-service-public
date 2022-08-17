package com.rarible.protocol.union.core.converter

import com.google.common.base.Utf8
import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.core.model.UnionOwnership
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
        )
    }

    fun convert(source: UnionOwnership): EsOwnership {
        val (id, original) = prepareOwnershipId(source.id.fullId())
        return EsOwnership(
            ownershipId = id,
            originalOwnershipId = original,
            blockchain = source.id.blockchain,
            itemId = source.id.getItemId().fullId(),
            collection = source.collection?.fullId(),
            owner = source.id.owner.fullId(),
            date = source.createdAt,
            auctionId = null,
            auctionOwnershipId = null,
        )
    }

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
