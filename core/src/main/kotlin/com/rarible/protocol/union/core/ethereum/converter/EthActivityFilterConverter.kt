package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.ActivityFilterAllDto
import com.rarible.protocol.dto.ActivityFilterAllTypeDto
import com.rarible.protocol.dto.ActivityFilterByCollectionDto
import com.rarible.protocol.dto.ActivityFilterByCollectionTypeDto
import com.rarible.protocol.dto.ActivityFilterByItemDto
import com.rarible.protocol.dto.ActivityFilterByItemTypeDto
import com.rarible.protocol.dto.ActivityFilterByUserDto
import com.rarible.protocol.dto.ActivityFilterByUserTypeDto
import com.rarible.protocol.dto.ActivityFilterDto
import com.rarible.protocol.dto.NftActivityFilterAllDto
import com.rarible.protocol.dto.NftActivityFilterByCollectionDto
import com.rarible.protocol.dto.NftActivityFilterByItemDto
import com.rarible.protocol.dto.NftActivityFilterByUserDto
import com.rarible.protocol.dto.NftActivityFilterDto
import com.rarible.protocol.dto.OrderActivityFilterAllDto
import com.rarible.protocol.dto.OrderActivityFilterByCollectionDto
import com.rarible.protocol.dto.OrderActivityFilterByItemDto
import com.rarible.protocol.dto.OrderActivityFilterByUserDto
import com.rarible.protocol.dto.OrderActivityFilterDto

object EthActivityFilterConverter {

    fun asItemActivityFilter(source: ActivityFilterDto): NftActivityFilterDto? {
        return when (source) {
            is ActivityFilterAllDto -> {
                val nftTypes = source.types.mapNotNull { type ->
                    when (type) {
                        ActivityFilterAllTypeDto.TRANSFER -> NftActivityFilterAllDto.Types.TRANSFER
                        ActivityFilterAllTypeDto.MINT -> NftActivityFilterAllDto.Types.MINT
                        ActivityFilterAllTypeDto.BURN -> NftActivityFilterAllDto.Types.BURN
                        ActivityFilterAllTypeDto.BID,
                        ActivityFilterAllTypeDto.LIST,
                        ActivityFilterAllTypeDto.SELL -> null
                    }
                }
                if (nftTypes.isNotEmpty()) NftActivityFilterAllDto(nftTypes) else null
            }
            is ActivityFilterByCollectionDto -> {
                val nftTypes = source.types.mapNotNull { type ->
                    when (type) {
                        ActivityFilterByCollectionTypeDto.TRANSFER -> NftActivityFilterByCollectionDto.Types.TRANSFER
                        ActivityFilterByCollectionTypeDto.MINT -> NftActivityFilterByCollectionDto.Types.MINT
                        ActivityFilterByCollectionTypeDto.BURN -> NftActivityFilterByCollectionDto.Types.BURN
                        ActivityFilterByCollectionTypeDto.BID,
                        ActivityFilterByCollectionTypeDto.LIST,
                        ActivityFilterByCollectionTypeDto.MATCH -> null
                    }
                }
                if (nftTypes.isNotEmpty()) NftActivityFilterByCollectionDto(source.contract, nftTypes) else null
            }
            is ActivityFilterByItemDto -> {
                val nftTypes = source.types.mapNotNull { type ->
                    when (type) {
                        ActivityFilterByItemTypeDto.TRANSFER -> NftActivityFilterByItemDto.Types.TRANSFER
                        ActivityFilterByItemTypeDto.MINT -> NftActivityFilterByItemDto.Types.MINT
                        ActivityFilterByItemTypeDto.BURN -> NftActivityFilterByItemDto.Types.BURN
                        ActivityFilterByItemTypeDto.BID,
                        ActivityFilterByItemTypeDto.LIST,
                        ActivityFilterByItemTypeDto.MATCH -> null
                    }
                }
                if (nftTypes.isNotEmpty()) NftActivityFilterByItemDto(
                    source.contract,
                    source.tokenId,
                    nftTypes
                ) else null
            }
            is ActivityFilterByUserDto -> {
                val nftTypes = source.types.mapNotNull { type ->
                    when (type) {
                        ActivityFilterByUserTypeDto.TRANSFER_FROM -> NftActivityFilterByUserDto.Types.TRANSFER_FROM
                        ActivityFilterByUserTypeDto.TRANSFER_TO -> NftActivityFilterByUserDto.Types.TRANSFER_TO
                        ActivityFilterByUserTypeDto.MINT -> NftActivityFilterByUserDto.Types.MINT
                        ActivityFilterByUserTypeDto.BURN -> NftActivityFilterByUserDto.Types.BURN
                        ActivityFilterByUserTypeDto.MAKE_BID,
                        ActivityFilterByUserTypeDto.GET_BID,
                        ActivityFilterByUserTypeDto.BUY,
                        ActivityFilterByUserTypeDto.LIST,
                        ActivityFilterByUserTypeDto.SELL -> null
                    }
                }
                if (nftTypes.isNotEmpty()) NftActivityFilterByUserDto(source.users, nftTypes) else null
            }
            else -> throw IllegalArgumentException("Unexpected activity filter type: $javaClass")
        }
    }

    fun asOrderActivityFilter(source: ActivityFilterDto): OrderActivityFilterDto? {
        return when (source) {
            is ActivityFilterAllDto -> {
                val orderTypes = source.types.mapNotNull { type ->
                    when (type) {
                        ActivityFilterAllTypeDto.BID -> OrderActivityFilterAllDto.Types.BID
                        ActivityFilterAllTypeDto.LIST -> OrderActivityFilterAllDto.Types.LIST
                        ActivityFilterAllTypeDto.SELL -> OrderActivityFilterAllDto.Types.MATCH
                        ActivityFilterAllTypeDto.TRANSFER,
                        ActivityFilterAllTypeDto.MINT,
                        ActivityFilterAllTypeDto.BURN -> null
                    }
                }
                if (orderTypes.isNotEmpty()) OrderActivityFilterAllDto(orderTypes) else null
            }
            is ActivityFilterByCollectionDto -> {
                val orderTypes = source.types.mapNotNull { type ->
                    when (type) {
                        ActivityFilterByCollectionTypeDto.BID -> OrderActivityFilterByCollectionDto.Types.BID
                        ActivityFilterByCollectionTypeDto.LIST -> OrderActivityFilterByCollectionDto.Types.LIST
                        ActivityFilterByCollectionTypeDto.MATCH -> OrderActivityFilterByCollectionDto.Types.MATCH
                        ActivityFilterByCollectionTypeDto.TRANSFER,
                        ActivityFilterByCollectionTypeDto.MINT,
                        ActivityFilterByCollectionTypeDto.BURN -> null
                    }
                }
                if (orderTypes.isNotEmpty()) OrderActivityFilterByCollectionDto(source.contract, orderTypes) else null
            }
            is ActivityFilterByItemDto -> {
                val orderTypes = source.types.mapNotNull { type ->
                    when (type) {
                        ActivityFilterByItemTypeDto.BID -> OrderActivityFilterByItemDto.Types.BID
                        ActivityFilterByItemTypeDto.LIST -> OrderActivityFilterByItemDto.Types.LIST
                        ActivityFilterByItemTypeDto.MATCH -> OrderActivityFilterByItemDto.Types.MATCH
                        ActivityFilterByItemTypeDto.TRANSFER,
                        ActivityFilterByItemTypeDto.MINT,
                        ActivityFilterByItemTypeDto.BURN -> null
                    }
                }
                if (orderTypes.isNotEmpty()) OrderActivityFilterByItemDto(
                    source.contract,
                    source.tokenId,
                    orderTypes
                ) else null
            }
            is ActivityFilterByUserDto -> {
                val nftTypes = source.types.mapNotNull { type ->
                    when (type) {
                        ActivityFilterByUserTypeDto.MAKE_BID -> OrderActivityFilterByUserDto.Types.MAKE_BID
                        ActivityFilterByUserTypeDto.GET_BID -> OrderActivityFilterByUserDto.Types.GET_BID
                        ActivityFilterByUserTypeDto.BUY -> OrderActivityFilterByUserDto.Types.BUY
                        ActivityFilterByUserTypeDto.LIST -> OrderActivityFilterByUserDto.Types.LIST
                        ActivityFilterByUserTypeDto.SELL -> OrderActivityFilterByUserDto.Types.SELL
                        ActivityFilterByUserTypeDto.TRANSFER_FROM,
                        ActivityFilterByUserTypeDto.TRANSFER_TO,
                        ActivityFilterByUserTypeDto.MINT,
                        ActivityFilterByUserTypeDto.BURN -> null
                    }
                }
                if (nftTypes.isNotEmpty()) OrderActivityFilterByUserDto(source.users, nftTypes) else null
            }
            else -> throw IllegalArgumentException("Unexpected activity filter type: $javaClass")
        }
    }
}