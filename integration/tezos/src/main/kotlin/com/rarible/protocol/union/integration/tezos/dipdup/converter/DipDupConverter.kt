package com.rarible.protocol.union.integration.tezos.dipdup.converter

import com.rarible.dipdup.client.core.model.Asset
import com.rarible.dipdup.client.core.model.TezosPlatform
import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.TezosFTAssetTypeDto
import com.rarible.protocol.union.dto.TezosMTAssetTypeDto
import com.rarible.protocol.union.dto.TezosNFTAssetTypeDto
import com.rarible.protocol.union.dto.TezosXTZAssetTypeDto

object DipDupConverter {

    fun convert(source: ActivitySortDto): com.rarible.protocol.tezos.dto.ActivitySortDto {
        return when (source) {
            ActivitySortDto.LATEST_FIRST -> com.rarible.protocol.tezos.dto.ActivitySortDto.LATEST_FIRST
            ActivitySortDto.EARLIEST_FIRST -> com.rarible.protocol.tezos.dto.ActivitySortDto.EARLIEST_FIRST
        }
    }

    fun convert(source: Asset, blockchain: BlockchainDto): AssetDto {
        return AssetDto(
            type = convert(source = source.assetType, blockchain = blockchain),
            value = source.assetValue
        )
    }

    fun convert(source: Asset.AssetType, blockchain: BlockchainDto): AssetTypeDto {
        return when (source) {
            is Asset.XTZ -> TezosXTZAssetTypeDto()
            is Asset.FT -> TezosFTAssetTypeDto(
                    contract = ContractAddressConverter.convert(blockchain, source.contract!!),
                    tokenId = source.tokenId
                )
            is Asset.MT -> TezosMTAssetTypeDto(
                contract = ContractAddressConverter.convert(blockchain, source.contract!!),
                tokenId = source.tokenId
            )
            is Asset.NFT -> TezosNFTAssetTypeDto(
                contract = ContractAddressConverter.convert(blockchain, source.contract!!),
                tokenId = source.tokenId
            )
            else -> TezosXTZAssetTypeDto()
        }
    }

    fun convert(source: TezosPlatform): PlatformDto {
        return when(source) {
            TezosPlatform.HEN -> PlatformDto.HEN
            TezosPlatform.OBJKT, TezosPlatform.OBJKT_V2 -> PlatformDto.OBJKT
            TezosPlatform.RARIBLE_V1, TezosPlatform.RARIBLE_V2 -> PlatformDto.RARIBLE
        }
    }
}
