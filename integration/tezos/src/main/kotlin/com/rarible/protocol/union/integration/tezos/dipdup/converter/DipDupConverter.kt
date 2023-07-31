package com.rarible.protocol.union.integration.tezos.dipdup.converter

import com.rarible.dipdup.client.core.model.Asset
import com.rarible.dipdup.client.core.model.TezosPlatform
import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.model.UnionAsset
import com.rarible.protocol.union.core.model.UnionAssetType
import com.rarible.protocol.union.core.model.UnionTezosFTAssetType
import com.rarible.protocol.union.core.model.UnionTezosMTAssetType
import com.rarible.protocol.union.core.model.UnionTezosNFTAssetType
import com.rarible.protocol.union.core.model.UnionTezosXTZAssetType
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.TezosFTAssetTypeDto
import com.rarible.protocol.union.dto.TezosMTAssetTypeDto
import com.rarible.protocol.union.dto.TezosNFTAssetTypeDto
import com.rarible.protocol.union.dto.TezosXTZAssetTypeDto

object DipDupConverter {

    fun convert(source: Asset, blockchain: BlockchainDto): UnionAsset {
        return UnionAsset(
            type = convert(source = source.assetType, blockchain = blockchain),
            value = source.assetValue
        )
    }

    fun convert(source: Asset.AssetType, blockchain: BlockchainDto): UnionAssetType {
        return when (source) {
            is Asset.XTZ -> UnionTezosXTZAssetType()
            is Asset.FT -> UnionTezosFTAssetType(
                contract = ContractAddressConverter.convert(blockchain, source.contract),
                tokenId = source.tokenId
            )

            is Asset.MT -> UnionTezosMTAssetType(
                contract = ContractAddressConverter.convert(blockchain, source.contract),
                tokenId = source.tokenId
            )

            is Asset.NFT -> UnionTezosNFTAssetType(
                contract = ContractAddressConverter.convert(blockchain, source.contract),
                tokenId = source.tokenId
            )

            else -> UnionTezosXTZAssetType()
        }
    }

    @Deprecated("remove after migration to UnionOrder")
    fun convertLegacy(source: Asset, blockchain: BlockchainDto): AssetDto {
        return AssetDto(
            type = convertLegacy(source = source.assetType, blockchain = blockchain),
            value = source.assetValue
        )
    }

    @Deprecated("remove after migration to UnionOrder")
    fun convertLegacy(source: Asset.AssetType, blockchain: BlockchainDto): AssetTypeDto {
        return when (source) {
            is Asset.XTZ -> TezosXTZAssetTypeDto()
            is Asset.FT -> TezosFTAssetTypeDto(
                contract = ContractAddressConverter.convert(blockchain, source.contract),
                tokenId = source.tokenId
            )

            is Asset.MT -> TezosMTAssetTypeDto(
                contract = ContractAddressConverter.convert(blockchain, source.contract),
                collection = CollectionIdDto(blockchain, source.contract),
                tokenId = source.tokenId
            )
            is Asset.NFT -> TezosNFTAssetTypeDto(
                contract = ContractAddressConverter.convert(blockchain, source.contract),
                collection = CollectionIdDto(blockchain, source.contract),
                tokenId = source.tokenId
            )
            else -> TezosXTZAssetTypeDto()
        }
    }

    fun convert(source: TezosPlatform): PlatformDto {
        return when (source) {
            TezosPlatform.HEN -> PlatformDto.HEN
            TezosPlatform.VERSUM_V1 -> PlatformDto.VERSUM
            TezosPlatform.TEIA_V1 -> PlatformDto.TEIA
            TezosPlatform.OBJKT_V1, TezosPlatform.OBJKT_V2 -> PlatformDto.OBJKT
            TezosPlatform.RARIBLE_V1, TezosPlatform.RARIBLE_V2 -> PlatformDto.RARIBLE
            TezosPlatform.FXHASH_V1, TezosPlatform.FXHASH_V2 -> PlatformDto.FXHASH
            else -> throw RuntimeException("Not implemented for $source platform")
        }
    }

    fun convert(source: PlatformDto?): List<TezosPlatform> {
        return when (source) {
            PlatformDto.HEN -> listOf(TezosPlatform.HEN)
            PlatformDto.VERSUM -> listOf(TezosPlatform.VERSUM_V1)
            PlatformDto.TEIA -> listOf(TezosPlatform.TEIA_V1)
            PlatformDto.OBJKT -> listOf(TezosPlatform.OBJKT_V1, TezosPlatform.OBJKT_V2)
            PlatformDto.RARIBLE -> listOf(TezosPlatform.RARIBLE_V1, TezosPlatform.RARIBLE_V2)
            PlatformDto.FXHASH -> listOf(TezosPlatform.FXHASH_V1, TezosPlatform.FXHASH_V2)
            else -> emptyList()
        }
    }
}
