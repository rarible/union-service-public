package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.model.UnionAsset
import com.rarible.protocol.union.core.model.UnionAssetType
import com.rarible.protocol.union.core.model.UnionEthAmmNftAssetType
import com.rarible.protocol.union.core.model.UnionEthCollectionAssetType
import com.rarible.protocol.union.core.model.UnionEthCryptoPunksAssetType
import com.rarible.protocol.union.core.model.UnionEthErc1155AssetType
import com.rarible.protocol.union.core.model.UnionEthErc1155LazyAssetType
import com.rarible.protocol.union.core.model.UnionEthErc20AssetType
import com.rarible.protocol.union.core.model.UnionEthErc721AssetType
import com.rarible.protocol.union.core.model.UnionEthErc721LazyAssetType
import com.rarible.protocol.union.core.model.UnionEthEthereumAssetType
import com.rarible.protocol.union.core.model.UnionEthGenerativeArtAssetType
import com.rarible.protocol.union.core.model.UnionFlowAssetTypeFt
import com.rarible.protocol.union.core.model.UnionFlowAssetTypeNft
import com.rarible.protocol.union.core.model.UnionSolanaFtAssetType
import com.rarible.protocol.union.core.model.UnionSolanaNftAssetType
import com.rarible.protocol.union.core.model.UnionSolanaSolAssetType
import com.rarible.protocol.union.core.model.UnionTezosFTAssetType
import com.rarible.protocol.union.core.model.UnionTezosMTAssetType
import com.rarible.protocol.union.core.model.UnionTezosNFTAssetType
import com.rarible.protocol.union.core.model.UnionTezosXTZAssetType
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.CollectionIdDto
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
import com.rarible.protocol.union.dto.FlowAssetTypeFtDto
import com.rarible.protocol.union.dto.FlowAssetTypeNftDto
import com.rarible.protocol.union.dto.SolanaFtAssetTypeDto
import com.rarible.protocol.union.dto.SolanaNftAssetTypeDto
import com.rarible.protocol.union.dto.SolanaSolAssetTypeDto
import com.rarible.protocol.union.dto.TezosFTAssetTypeDto
import com.rarible.protocol.union.dto.TezosMTAssetTypeDto
import com.rarible.protocol.union.dto.TezosNFTAssetTypeDto
import com.rarible.protocol.union.dto.TezosXTZAssetTypeDto
import com.rarible.protocol.union.enrichment.converter.data.EnrichmentAssetData

@Suppress("UNUSED_PARAMETER")
object AssetDtoConverter {

    fun convert(
        source: UnionAsset,
        data: EnrichmentAssetData = EnrichmentAssetData.empty()
    ): AssetDto {
        return AssetDto(
            type = convert(source.type, data),
            value = source.value
        )
    }

    fun convert(
        source: UnionAssetType,
        data: EnrichmentAssetData = EnrichmentAssetData.empty()
    ): AssetTypeDto {
        return when (source) {
            is UnionEthAmmNftAssetType -> convert(source, data)
            is UnionEthCollectionAssetType -> convert(source, data)
            is UnionEthCryptoPunksAssetType -> convert(source, data)
            is UnionEthErc1155AssetType -> convert(source, data)
            is UnionEthErc1155LazyAssetType -> convert(source, data)
            is UnionEthErc20AssetType -> convert(source, data)
            is UnionEthErc721AssetType -> convert(source, data)
            is UnionEthErc721LazyAssetType -> convert(source, data)
            is UnionEthEthereumAssetType -> convert(source, data)
            is UnionEthGenerativeArtAssetType -> convert(source, data)
            is UnionFlowAssetTypeFt -> convert(source, data)
            is UnionFlowAssetTypeNft -> convert(source, data)
            is UnionSolanaFtAssetType -> convert(source, data)
            is UnionSolanaNftAssetType -> convert(source, data)
            is UnionSolanaSolAssetType -> convert(source, data)
            is UnionTezosFTAssetType -> convert(source, data)
            is UnionTezosMTAssetType -> convert(source, data)
            is UnionTezosNFTAssetType -> convert(source, data)
            is UnionTezosXTZAssetType -> convert(source, data)
        }
    }

    private fun convert(
        source: UnionEthAmmNftAssetType,
        data: EnrichmentAssetData
    ): EthAmmNftAssetTypeDto {
        return EthAmmNftAssetTypeDto(
            contract = source.contract,
            collection = source.getEnrichedCollection(data)
        )
    }

    private fun convert(
        source: UnionEthCollectionAssetType,
        data: EnrichmentAssetData
    ): EthCollectionAssetTypeDto {
        return EthCollectionAssetTypeDto(
            contract = source.contract,
            collection = source.getEnrichedCollection(data)
        )
    }

    private fun convert(
        source: UnionEthCryptoPunksAssetType,
        data: EnrichmentAssetData
    ): EthCryptoPunksAssetTypeDto {
        return EthCryptoPunksAssetTypeDto(
            contract = source.contract,
            tokenId = source.tokenId,
            collection = source.getEnrichedCollection(data),
        )
    }

    private fun convert(
        source: UnionEthErc1155AssetType,
        data: EnrichmentAssetData
    ): EthErc1155AssetTypeDto {
        return EthErc1155AssetTypeDto(
            contract = source.contract,
            tokenId = source.tokenId,
            collection = source.getEnrichedCollection(data),
        )
    }

    private fun convert(
        source: UnionEthErc1155LazyAssetType,
        data: EnrichmentAssetData
    ): EthErc1155LazyAssetTypeDto {
        return EthErc1155LazyAssetTypeDto(
            contract = source.contract,
            tokenId = source.tokenId,
            collection = source.getEnrichedCollection(data),
            uri = source.uri,
            supply = source.supply,
            creators = source.creators,
            royalties = source.royalties,
            signatures = source.signatures
        )
    }

    private fun convert(
        source: UnionEthErc20AssetType,
        data: EnrichmentAssetData
    ): EthErc20AssetTypeDto {
        return EthErc20AssetTypeDto(
            contract = source.contract
        )
    }

    private fun convert(
        source: UnionEthErc721AssetType,
        data: EnrichmentAssetData
    ): EthErc721AssetTypeDto {
        return EthErc721AssetTypeDto(
            contract = source.contract,
            tokenId = source.tokenId,
            collection = source.getEnrichedCollection(data)
        )
    }

    private fun convert(
        source: UnionEthErc721LazyAssetType,
        data: EnrichmentAssetData
    ): EthErc721LazyAssetTypeDto {
        return EthErc721LazyAssetTypeDto(
            contract = source.contract,
            tokenId = source.tokenId,
            collection = source.getEnrichedCollection(data),
            uri = source.uri,
            creators = source.creators,
            royalties = source.royalties,
            signatures = source.signatures
        )
    }

    private fun convert(
        source: UnionEthEthereumAssetType,
        data: EnrichmentAssetData
    ): EthEthereumAssetTypeDto {
        return EthEthereumAssetTypeDto(
            blockchain = source.blockchain,
        )
    }

    private fun convert(
        source: UnionEthGenerativeArtAssetType,
        data: EnrichmentAssetData
    ): EthGenerativeArtAssetTypeDto {
        return EthGenerativeArtAssetTypeDto(
            contract = source.contract,
            collection = source.getEnrichedCollection(data)
        )
    }

    private fun convert(
        source: UnionFlowAssetTypeFt,
        data: EnrichmentAssetData
    ): FlowAssetTypeFtDto {
        return FlowAssetTypeFtDto(
            contract = source.contract
        )
    }

    private fun convert(
        source: UnionFlowAssetTypeNft,
        data: EnrichmentAssetData
    ): FlowAssetTypeNftDto {
        return FlowAssetTypeNftDto(
            contract = source.contract,
            tokenId = source.tokenId,
            collection = source.getEnrichedCollection(data)
        )
    }

    private fun convert(
        source: UnionSolanaFtAssetType,
        data: EnrichmentAssetData
    ): SolanaFtAssetTypeDto {
        return SolanaFtAssetTypeDto(
            address = source.address
        )
    }

    private fun convert(
        source: UnionSolanaNftAssetType,
        data: EnrichmentAssetData
    ): SolanaNftAssetTypeDto {
        return SolanaNftAssetTypeDto(
            contract = source.contract,
            itemId = source.itemId,
            collection = source.getEnrichedCollection(data)
        )
    }

    private fun convert(
        source: UnionSolanaSolAssetType,
        data: EnrichmentAssetData
    ): SolanaSolAssetTypeDto {
        return SolanaSolAssetTypeDto()
    }

    private fun convert(
        source: UnionTezosFTAssetType,
        data: EnrichmentAssetData
    ): TezosFTAssetTypeDto {
        return TezosFTAssetTypeDto(
            contract = source.contract,
            tokenId = source.tokenId
        )
    }

    private fun convert(
        source: UnionTezosMTAssetType,
        data: EnrichmentAssetData
    ): TezosMTAssetTypeDto {
        return TezosMTAssetTypeDto(
            contract = source.contract,
            tokenId = source.tokenId,
            collection = source.getEnrichedCollection(data)
        )
    }

    private fun convert(
        source: UnionTezosNFTAssetType,
        data: EnrichmentAssetData
    ): TezosNFTAssetTypeDto {
        return TezosNFTAssetTypeDto(
            contract = source.contract,
            tokenId = source.tokenId,
            collection = source.getEnrichedCollection(data)
        )
    }

    private fun convert(
        source: UnionTezosXTZAssetType,
        data: EnrichmentAssetData
    ): TezosXTZAssetTypeDto {
        return TezosXTZAssetTypeDto()
    }

    private fun UnionAssetType.getEnrichedCollection(data: EnrichmentAssetData): CollectionIdDto? {
        return data.customCollection ?: this.collectionId()
    }

}