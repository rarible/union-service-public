package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.enrichment.model.CurrencyId
import com.rarible.protocol.union.enrichment.model.CurrencyType

object CurrencyIdConverter {

    fun convert(
        blockchain: BlockchainDto,
        assetType: AssetTypeDto
    ): CurrencyId {
        return when (assetType) {
            is EthCryptoPunksAssetTypeDto -> CurrencyId(
                blockchain,
                assetType.contract.value,
                CurrencyType.PUNK
            )
            is EthErc1155AssetTypeDto -> CurrencyId(
                blockchain,
                assetType.contract.value,
                CurrencyType.ERC1155
            )
            is EthErc1155LazyAssetTypeDto -> CurrencyId(
                blockchain,
                assetType.contract.value,
                CurrencyType.ERC1155_LAZY
            )
            is EthErc20AssetTypeDto -> CurrencyId(
                blockchain,
                assetType.contract.value,
                CurrencyType.ERC20
            )
            is EthErc721AssetTypeDto -> CurrencyId(
                blockchain,
                assetType.contract.value,
                CurrencyType.ERC721
            )
            is EthErc721LazyAssetTypeDto -> CurrencyId(
                blockchain,
                assetType.contract.value,
                CurrencyType.ERC721_LAZY
            )
            is EthEthereumAssetTypeDto -> CurrencyId(
                blockchain,
                "",
                CurrencyType.NATIVE
            )
            is EthGenerativeArtAssetTypeDto -> CurrencyId(
                blockchain,
                assetType.contract.value,
                CurrencyType.GEN_ART
            )
            is FlowAssetTypeFtDto -> CurrencyId(
                blockchain,
                assetType.contract.value,
                CurrencyType.FT
            )
            is FlowAssetTypeNftDto -> CurrencyId(
                blockchain,
                assetType.contract.value,
                CurrencyType.NFT
            )
        }
    }
}
