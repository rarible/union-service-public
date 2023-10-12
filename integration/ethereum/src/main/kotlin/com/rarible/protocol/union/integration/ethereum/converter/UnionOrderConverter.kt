package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.protocol.dto.AmmNftAssetTypeDto
import com.rarible.protocol.dto.AssetTypeDto
import com.rarible.protocol.dto.CollectionAssetTypeDto
import com.rarible.protocol.dto.CryptoPunksAssetTypeDto
import com.rarible.protocol.dto.Erc1155AssetTypeDto
import com.rarible.protocol.dto.Erc1155LazyAssetTypeDto
import com.rarible.protocol.dto.Erc20AssetTypeDto
import com.rarible.protocol.dto.Erc721AssetTypeDto
import com.rarible.protocol.dto.Erc721LazyAssetTypeDto
import com.rarible.protocol.dto.EthAssetTypeDto
import com.rarible.protocol.dto.GenerativeArtAssetTypeDto
import com.rarible.protocol.dto.OrderFormAssetDto
import com.rarible.protocol.dto.OrderRaribleV2DataDto
import com.rarible.protocol.dto.OrderRaribleV2DataV1Dto
import com.rarible.protocol.dto.OrderRaribleV2DataV2Dto
import com.rarible.protocol.dto.OrderRaribleV2DataV3BuyDto
import com.rarible.protocol.dto.OrderRaribleV2DataV3SellDto
import com.rarible.protocol.union.core.exception.UnionException
import com.rarible.protocol.union.dto.EthAmmNftAssetTypeDto
import com.rarible.protocol.union.dto.EthCollectionAssetTypeDto
import com.rarible.protocol.union.dto.EthCryptoPunksAssetTypeDto
import com.rarible.protocol.union.dto.EthErc1155AssetTypeDto
import com.rarible.protocol.union.dto.EthErc1155LazyAssetTypeDto
import com.rarible.protocol.union.dto.EthErc20AssetTypeDto
import com.rarible.protocol.union.dto.EthErc721AssetTypeDto
import com.rarible.protocol.union.dto.EthErc721LazyAssetTypeDto
import com.rarible.protocol.union.dto.EthEthereumAssetTypeDto
import com.rarible.protocol.union.dto.EthGenerativeArtAssetTypeDto
import com.rarible.protocol.union.dto.EthOrderDataRaribleV2DataV1Dto
import com.rarible.protocol.union.dto.EthOrderDataRaribleV2DataV2Dto
import com.rarible.protocol.union.dto.EthOrderDataRaribleV2DataV3BuyDto
import com.rarible.protocol.union.dto.EthOrderDataRaribleV2DataV3SellDto
import com.rarible.protocol.union.dto.EthOrderFormAssetDto
import com.rarible.protocol.union.dto.EthRaribleOrderFormDto
import com.rarible.protocol.union.dto.EthRaribleV2OrderDataDto
import com.rarible.protocol.union.dto.EthRaribleV2OrderFormDto

object UnionOrderConverter {

    fun convert(source: EthRaribleOrderFormDto): com.rarible.protocol.dto.OrderFormDto {
        return when (source) {
            is EthRaribleV2OrderFormDto -> com.rarible.protocol.dto.RaribleV2OrderFormDto(
                maker = EthConverter.convertToAddress(source.maker.value),
                taker = source.taker?.let { EthConverter.convertToAddress(it.value) },
                make = convert(source.make),
                take = convert(source.take),
                salt = source.salt,
                start = source.startedAt?.epochSecond,
                end = source.endedAt.epochSecond,
                signature = EthConverter.convertToBinary(source.signature),
                data = convert(source.data)
            )
        }
    }

    fun convert(source: EthRaribleV2OrderDataDto): OrderRaribleV2DataDto {
        return when (source) {
            is EthOrderDataRaribleV2DataV1Dto -> {
                OrderRaribleV2DataV1Dto(
                    payouts = source.payouts.map { EthConverter.convertToPart(it) },
                    originFees = source.originFees.map { EthConverter.convertToPart(it) },
                )
            }

            is EthOrderDataRaribleV2DataV2Dto -> {
                OrderRaribleV2DataV2Dto(
                    payouts = source.payouts.map { EthConverter.convertToPart(it) },
                    originFees = source.originFees.map { EthConverter.convertToPart(it) },
                    isMakeFill = source.isMakeFill
                )
            }

            is EthOrderDataRaribleV2DataV3SellDto -> {
                OrderRaribleV2DataV3SellDto(
                    payout = source.payout?.let { EthConverter.convertToPart(it) },
                    originFeeFirst = source.originFeeFirst?.let { EthConverter.convertToPart(it) },
                    originFeeSecond = source.originFeeSecond?.let { EthConverter.convertToPart(it) },
                    maxFeesBasePoint = source.maxFeesBasePoint,
                    marketplaceMarker = source.marketplaceMarker?.let { EthConverter.convertToWord(it) }
                )
            }

            is EthOrderDataRaribleV2DataV3BuyDto -> {
                OrderRaribleV2DataV3BuyDto(
                    payout = source.payout?.let { EthConverter.convertToPart(it) },
                    originFeeFirst = source.originFeeFirst?.let { EthConverter.convertToPart(it) },
                    originFeeSecond = source.originFeeSecond?.let { EthConverter.convertToPart(it) },
                    marketplaceMarker = source.marketplaceMarker?.let { EthConverter.convertToWord(it) }
                )
            }
        }
    }

    private fun convert(source: EthOrderFormAssetDto): OrderFormAssetDto {
        return OrderFormAssetDto(
            assetType = convert(source.assetType),
            value = source.value
        )
    }

    private fun convert(source: com.rarible.protocol.union.dto.AssetTypeDto): AssetTypeDto {
        return when (source) {
            is EthEthereumAssetTypeDto -> EthAssetTypeDto()
            is EthErc20AssetTypeDto -> Erc20AssetTypeDto(
                contract = EthConverter.convertToAddress(source.contract.value)
            )

            is EthErc721AssetTypeDto -> Erc721AssetTypeDto(
                contract = EthConverter.convertToAddress(source.contract.value),
                tokenId = source.tokenId
            )

            is EthErc1155AssetTypeDto -> Erc1155AssetTypeDto(
                contract = EthConverter.convertToAddress(source.contract.value),
                tokenId = source.tokenId
            )

            is EthErc721LazyAssetTypeDto -> Erc721LazyAssetTypeDto(
                contract = EthConverter.convertToAddress(source.contract.value),
                tokenId = source.tokenId,
                uri = source.uri,
                creators = source.creators.map { EthConverter.convertToPart(it) },
                royalties = source.royalties.map { EthConverter.convertToPart(it) },
                signatures = source.signatures.map { EthConverter.convertToBinary(it) }
            )

            is EthErc1155LazyAssetTypeDto -> Erc1155LazyAssetTypeDto(
                contract = EthConverter.convertToAddress(source.contract.value),
                tokenId = source.tokenId,
                uri = source.uri,
                supply = source.supply,
                creators = source.creators.map { EthConverter.convertToPart(it) },
                royalties = source.royalties.map { EthConverter.convertToPart(it) },
                signatures = source.signatures.map { EthConverter.convertToBinary(it) }
            )

            is EthCryptoPunksAssetTypeDto -> CryptoPunksAssetTypeDto(
                contract = EthConverter.convertToAddress(source.contract.value),
                tokenId = source.tokenId
            )

            is EthGenerativeArtAssetTypeDto -> GenerativeArtAssetTypeDto(
                contract = EthConverter.convertToAddress(source.contract.value),
            )

            is EthCollectionAssetTypeDto -> CollectionAssetTypeDto(
                contract = EthConverter.convertToAddress(source.contract.value),
            )

            is EthAmmNftAssetTypeDto -> AmmNftAssetTypeDto(
                contract = EthConverter.convertToAddress(source.contract.value),
            )

            else -> throw UnionException("Unsupported Eth AssetType: ${source.javaClass.simpleName}")
        }
    }
}
