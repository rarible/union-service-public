package com.rarible.protocol.union.integration.solana.converter

import com.rarible.protocol.solana.dto.ActivityFilterAllTypeDto
import com.rarible.protocol.solana.dto.ActivityFilterByCollectionTypeDto
import com.rarible.protocol.solana.dto.ActivityFilterByItemTypeDto
import com.rarible.protocol.solana.dto.ActivityFilterByUserTypeDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.ActivityBlockchainInfoDto
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BurnActivityDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MintActivityDto
import com.rarible.protocol.union.dto.TransferActivityDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import org.springframework.stereotype.Component

@Component
class SolanaActivityConverter {

    fun convert(source: com.rarible.protocol.solana.dto.ActivityDto, blockchain: BlockchainDto): ActivityDto {
        val activityId = ActivityIdDto(blockchain, source.id)
        return when (source) {
            is com.rarible.protocol.solana.dto.MintActivityDto -> {
                MintActivityDto(
                    id = activityId,
                    date = source.date,
                    owner = UnionAddressConverter.convert(blockchain, source.owner),
                    itemId = ItemIdDto(blockchain, source.tokenAddress),
                    value = source.value,
                    transactionHash = source.blockchainInfo.transactionHash,
                    // TODO UNION remove in 1.19
                    blockchainInfo = convert(source.blockchainInfo),
                    reverted = source.reverted
                )
            }
            is com.rarible.protocol.solana.dto.BurnActivityDto -> {
                BurnActivityDto(
                    id = activityId,
                    date = source.date,
                    owner = UnionAddressConverter.convert(blockchain, source.owner),
                    itemId = ItemIdDto(blockchain, source.tokenAddress),
                    value = source.value,
                    transactionHash = source.blockchainInfo.transactionHash,
                    // TODO UNION remove in 1.19
                    blockchainInfo = convert(source.blockchainInfo),
                    reverted = source.reverted
                )
            }
            is com.rarible.protocol.solana.dto.TransferActivityDto -> {
                TransferActivityDto(
                    id = activityId,
                    date = source.date,
                    from = UnionAddressConverter.convert(blockchain, source.from),
                    owner = UnionAddressConverter.convert(blockchain, source.owner),
                    itemId = source.tokenAddress?.let { ItemIdDto(blockchain, it) }, // TODO SOLANA
                    value = source.value,
                    purchase = source.purchase,
                    transactionHash = source.blockchainInfo.transactionHash,
                    // TODO UNION remove in 1.19
                    blockchainInfo = convert(source.blockchainInfo),
                    reverted = source.reverted
                )
            }
        }
    }

    fun convertToAllTypes(type: ActivityTypeDto): ActivityFilterAllTypeDto? {
        return when (type) {
            ActivityTypeDto.TRANSFER -> ActivityFilterAllTypeDto.TRANSFER
            ActivityTypeDto.MINT -> ActivityFilterAllTypeDto.MINT
            ActivityTypeDto.BURN -> ActivityFilterAllTypeDto.BURN
            ActivityTypeDto.BID -> ActivityFilterAllTypeDto.BID
            ActivityTypeDto.LIST -> ActivityFilterAllTypeDto.LIST
            ActivityTypeDto.SELL -> ActivityFilterAllTypeDto.SELL
            ActivityTypeDto.CANCEL_LIST -> ActivityFilterAllTypeDto.CANCEL_LIST
            ActivityTypeDto.CANCEL_BID -> ActivityFilterAllTypeDto.CANCEL_BID
            else -> null
        }
    }

    fun convertToCollectionTypes(type: ActivityTypeDto): ActivityFilterByCollectionTypeDto? {
        return when (type) {
            ActivityTypeDto.TRANSFER -> ActivityFilterByCollectionTypeDto.TRANSFER
            ActivityTypeDto.MINT -> ActivityFilterByCollectionTypeDto.MINT
            ActivityTypeDto.BURN -> ActivityFilterByCollectionTypeDto.BURN
            ActivityTypeDto.BID -> ActivityFilterByCollectionTypeDto.BID
            ActivityTypeDto.LIST -> ActivityFilterByCollectionTypeDto.LIST
            ActivityTypeDto.SELL -> ActivityFilterByCollectionTypeDto.SELL
            ActivityTypeDto.CANCEL_LIST -> ActivityFilterByCollectionTypeDto.CANCEL_LIST
            ActivityTypeDto.CANCEL_BID -> ActivityFilterByCollectionTypeDto.CANCEL_BID
            else -> null
        }
    }

    fun convertToItemTypes(type: ActivityTypeDto): ActivityFilterByItemTypeDto? {
        return when (type) {
            ActivityTypeDto.TRANSFER -> ActivityFilterByItemTypeDto.TRANSFER
            ActivityTypeDto.MINT -> ActivityFilterByItemTypeDto.MINT
            ActivityTypeDto.BURN -> ActivityFilterByItemTypeDto.BURN
            ActivityTypeDto.BID -> ActivityFilterByItemTypeDto.BID
            ActivityTypeDto.LIST -> ActivityFilterByItemTypeDto.LIST
            ActivityTypeDto.SELL -> ActivityFilterByItemTypeDto.SELL
            ActivityTypeDto.CANCEL_LIST -> ActivityFilterByItemTypeDto.CANCEL_LIST
            ActivityTypeDto.CANCEL_BID -> ActivityFilterByItemTypeDto.CANCEL_BID
            else -> null
        }
    }

    fun convertToUserTypes(type: UserActivityTypeDto): ActivityFilterByUserTypeDto? {
        return when (type) {
            UserActivityTypeDto.TRANSFER_FROM -> ActivityFilterByUserTypeDto.TRANSFER_FROM
            UserActivityTypeDto.TRANSFER_TO -> ActivityFilterByUserTypeDto.TRANSFER_TO
            UserActivityTypeDto.MINT -> ActivityFilterByUserTypeDto.MINT
            UserActivityTypeDto.BURN -> ActivityFilterByUserTypeDto.BURN
            UserActivityTypeDto.MAKE_BID -> ActivityFilterByUserTypeDto.MAKE_BID
            UserActivityTypeDto.GET_BID -> ActivityFilterByUserTypeDto.GET_BID
            UserActivityTypeDto.LIST -> ActivityFilterByUserTypeDto.LIST
            UserActivityTypeDto.BUY -> ActivityFilterByUserTypeDto.BUY
            UserActivityTypeDto.SELL -> ActivityFilterByUserTypeDto.SELL
            UserActivityTypeDto.CANCEL_LIST -> ActivityFilterByUserTypeDto.CANCEL_LIST
            UserActivityTypeDto.CANCEL_BID -> ActivityFilterByUserTypeDto.CANCEL_BID
            else -> null
        }
    }

    private fun convert(
        source: com.rarible.protocol.solana.dto.ActivityBlockchainInfoDto?
    ): ActivityBlockchainInfoDto? {
        if (source == null) return null
        return ActivityBlockchainInfoDto(
            transactionHash = source.transactionHash,
            blockHash = source.blockHash,
            blockNumber = source.blockNumber,
            logIndex = source.transactionIndex
        )
    }

}