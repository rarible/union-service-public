package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.*

object EthActivityFilterConverter {

    fun asItemActivityFilter(source: ActivityFilterDto): NftActivityFilterDto? {
        return when (source) {
            is ActivityFilterAllDto -> {
                val nftTypes = source.types.mapNotNull { type ->
                    when (type) {
                        ActivityFilterAllDto.Types.TRANSFER -> NftActivityFilterAllDto.Types.TRANSFER
                        ActivityFilterAllDto.Types.MINT -> NftActivityFilterAllDto.Types.MINT
                        ActivityFilterAllDto.Types.BURN -> NftActivityFilterAllDto.Types.BURN
                        ActivityFilterAllDto.Types.BID,
                        ActivityFilterAllDto.Types.LIST,
                        ActivityFilterAllDto.Types.SELL -> null
                    }
                }
                if (nftTypes.isNotEmpty()) NftActivityFilterAllDto(nftTypes) else null
            }
            is ActivityFilterByCollectionDto -> {
                val nftTypes = source.types.mapNotNull { type ->
                    when (type) {
                        ActivityFilterByCollectionDto.Types.TRANSFER -> NftActivityFilterByCollectionDto.Types.TRANSFER
                        ActivityFilterByCollectionDto.Types.MINT -> NftActivityFilterByCollectionDto.Types.MINT
                        ActivityFilterByCollectionDto.Types.BURN -> NftActivityFilterByCollectionDto.Types.BURN
                        ActivityFilterByCollectionDto.Types.BID,
                        ActivityFilterByCollectionDto.Types.LIST,
                        ActivityFilterByCollectionDto.Types.MATCH -> null
                    }
                }
                if (nftTypes.isNotEmpty()) NftActivityFilterByCollectionDto(source.contract, nftTypes) else null
            }
            is ActivityFilterByItemDto -> {
                val nftTypes = source.types.mapNotNull { type ->
                    when (type) {
                        ActivityFilterByItemDto.Types.TRANSFER -> NftActivityFilterByItemDto.Types.TRANSFER
                        ActivityFilterByItemDto.Types.MINT -> NftActivityFilterByItemDto.Types.MINT
                        ActivityFilterByItemDto.Types.BURN -> NftActivityFilterByItemDto.Types.BURN
                        ActivityFilterByItemDto.Types.BID,
                        ActivityFilterByItemDto.Types.LIST,
                        ActivityFilterByItemDto.Types.MATCH -> null
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
                        ActivityFilterByUserDto.Types.TRANSFER_FROM -> NftActivityFilterByUserDto.Types.TRANSFER_FROM
                        ActivityFilterByUserDto.Types.TRANSFER_TO -> NftActivityFilterByUserDto.Types.TRANSFER_TO
                        ActivityFilterByUserDto.Types.MINT -> NftActivityFilterByUserDto.Types.MINT
                        ActivityFilterByUserDto.Types.BURN -> NftActivityFilterByUserDto.Types.BURN
                        ActivityFilterByUserDto.Types.MAKE_BID,
                        ActivityFilterByUserDto.Types.GET_BID,
                        ActivityFilterByUserDto.Types.BUY,
                        ActivityFilterByUserDto.Types.LIST,
                        ActivityFilterByUserDto.Types.SELL -> null
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
                        ActivityFilterAllDto.Types.BID -> OrderActivityFilterAllDto.Types.BID
                        ActivityFilterAllDto.Types.LIST -> OrderActivityFilterAllDto.Types.LIST
                        ActivityFilterAllDto.Types.SELL -> OrderActivityFilterAllDto.Types.MATCH
                        ActivityFilterAllDto.Types.TRANSFER,
                        ActivityFilterAllDto.Types.MINT,
                        ActivityFilterAllDto.Types.BURN -> null
                    }
                }
                if (orderTypes.isNotEmpty()) OrderActivityFilterAllDto(orderTypes) else null
            }
            is ActivityFilterByCollectionDto -> {
                val orderTypes = source.types.mapNotNull { type ->
                    when (type) {
                        ActivityFilterByCollectionDto.Types.BID -> OrderActivityFilterByCollectionDto.Types.BID
                        ActivityFilterByCollectionDto.Types.LIST -> OrderActivityFilterByCollectionDto.Types.LIST
                        ActivityFilterByCollectionDto.Types.MATCH -> OrderActivityFilterByCollectionDto.Types.MATCH
                        ActivityFilterByCollectionDto.Types.TRANSFER,
                        ActivityFilterByCollectionDto.Types.MINT,
                        ActivityFilterByCollectionDto.Types.BURN -> null
                    }
                }
                if (orderTypes.isNotEmpty()) OrderActivityFilterByCollectionDto(source.contract, orderTypes) else null
            }
            is ActivityFilterByItemDto -> {
                val orderTypes = source.types.mapNotNull { type ->
                    when (type) {
                        ActivityFilterByItemDto.Types.BID -> OrderActivityFilterByItemDto.Types.BID
                        ActivityFilterByItemDto.Types.LIST -> OrderActivityFilterByItemDto.Types.LIST
                        ActivityFilterByItemDto.Types.MATCH -> OrderActivityFilterByItemDto.Types.MATCH
                        ActivityFilterByItemDto.Types.TRANSFER,
                        ActivityFilterByItemDto.Types.MINT,
                        ActivityFilterByItemDto.Types.BURN -> null
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
                        ActivityFilterByUserDto.Types.MAKE_BID -> OrderActivityFilterByUserDto.Types.MAKE_BID
                        ActivityFilterByUserDto.Types.GET_BID -> OrderActivityFilterByUserDto.Types.GET_BID
                        ActivityFilterByUserDto.Types.BUY -> OrderActivityFilterByUserDto.Types.BUY
                        ActivityFilterByUserDto.Types.LIST -> OrderActivityFilterByUserDto.Types.LIST
                        ActivityFilterByUserDto.Types.SELL -> OrderActivityFilterByUserDto.Types.SELL
                        ActivityFilterByUserDto.Types.TRANSFER_FROM,
                        ActivityFilterByUserDto.Types.TRANSFER_TO,
                        ActivityFilterByUserDto.Types.MINT,
                        ActivityFilterByUserDto.Types.BURN -> null
                    }
                }
                if (nftTypes.isNotEmpty()) OrderActivityFilterByUserDto(source.users, nftTypes) else null
            }
            else -> throw IllegalArgumentException("Unexpected activity filter type: $javaClass")
        }
    }
}