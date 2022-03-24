package com.rarible.protocol.union.search.core.converter

import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.AuctionBidActivityDto
import com.rarible.protocol.union.dto.AuctionCancelActivityDto
import com.rarible.protocol.union.dto.AuctionEndActivityDto
import com.rarible.protocol.union.dto.AuctionFinishActivityDto
import com.rarible.protocol.union.dto.AuctionOpenActivityDto
import com.rarible.protocol.union.dto.AuctionStartActivityDto
import com.rarible.protocol.union.dto.BurnActivityDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.OrderBidActivityDto
import com.rarible.protocol.union.dto.OrderCancelBidActivityDto
import com.rarible.protocol.union.dto.OrderCancelListActivityDto
import com.rarible.protocol.union.dto.OrderListActivityDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.OrderMatchSwapDto
import com.rarible.protocol.union.dto.TransferActivityDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.search.core.ElasticActivity
import org.springframework.stereotype.Service
import java.math.BigInteger

@Service
class ElasticActivityConverter {

    fun convert(source: ActivityDto): ElasticActivity {
        return when (source) {
            is MintActivityDto -> convertMint(source)
            is BurnActivityDto -> TODO()
            is TransferActivityDto -> TODO()
            is OrderMatchSwapDto -> TODO()
            is OrderMatchSellDto -> TODO()
            is OrderBidActivityDto -> TODO()
            is OrderListActivityDto -> TODO()
            is OrderCancelBidActivityDto -> TODO()
            is OrderCancelListActivityDto -> TODO()
            is AuctionOpenActivityDto -> TODO()
            is AuctionBidActivityDto -> TODO()
            is AuctionFinishActivityDto -> TODO()
            is AuctionCancelActivityDto -> TODO()
            is AuctionStartActivityDto -> TODO()
            is AuctionEndActivityDto -> TODO()
        }
    }

    private fun convertMint(source: MintActivityDto): ElasticActivity {
        val itemId = safeItemId(source.contract, source.tokenId, source.itemId)
        return ElasticActivity(
            activityId = source.id.value,
            date = source.date,
            blockNumber = source.blockchainInfo!!.blockNumber,
            logIndex = source.blockchainInfo!!.logIndex,
            blockchain = source.id.blockchain,
            type = ActivityTypeDto.MINT,
            user = singleUser(source.owner),
            collection = singleCollection(itemId),
            item = singleItem(itemId)
        )
    }

    private fun singleUser(user: UnionAddress): ElasticActivity.User {
        return ElasticActivity.User(
            maker = user.value,
            taker = null,
        )
    }

    private fun singleCollection(itemId: ItemIdDto): ElasticActivity.Collection {
        return ElasticActivity.Collection(
            make = itemId.value.split(":").first(),
            take = null,
        )
    }

    private fun singleItem(itemId: ItemIdDto): ElasticActivity.Item {
        return ElasticActivity.Item(
            make = itemId.value,
            take = null,
        )
    }

    private fun safeItemId(
        contract: ContractAddress?,
        tokenId: BigInteger?,
        itemId: ItemIdDto?,
    ): ItemIdDto {
        if (itemId != null) return itemId
        if (contract != null && tokenId != null) {
            return ItemIdDto(
                blockchain = contract.blockchain,
                contract = contract.value,
                tokenId = tokenId,
            )
        }
        throw IllegalArgumentException("contract & tokenId & itemId fields are null")
    }
}
