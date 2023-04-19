package com.rarible.protocol.union.enrichment.converter

import com.rarible.protocol.union.core.model.UnionAssetDto
import com.rarible.protocol.union.core.model.UnionAssetTypeDto
import com.rarible.protocol.union.core.model.UnionEthAmmNftAssetTypeDto
import com.rarible.protocol.union.core.model.UnionEthCollectionAssetTypeDto
import com.rarible.protocol.union.core.model.UnionEthCryptoPunksAssetTypeDto
import com.rarible.protocol.union.core.model.UnionEthErc1155AssetTypeDto
import com.rarible.protocol.union.core.model.UnionEthErc1155LazyAssetTypeDto
import com.rarible.protocol.union.core.model.UnionEthErc20AssetTypeDto
import com.rarible.protocol.union.core.model.UnionEthErc721AssetTypeDto
import com.rarible.protocol.union.core.model.UnionEthErc721LazyAssetTypeDto
import com.rarible.protocol.union.core.model.UnionEthEthereumAssetTypeDto
import com.rarible.protocol.union.core.model.UnionEthGenerativeArtAssetTypeDto
import com.rarible.protocol.union.core.model.UnionFlowAssetTypeFtDto
import com.rarible.protocol.union.core.model.UnionFlowAssetTypeNftDto
import com.rarible.protocol.union.core.model.UnionSolanaFtAssetTypeDto
import com.rarible.protocol.union.core.model.UnionSolanaNftAssetTypeDto
import com.rarible.protocol.union.core.model.UnionSolanaSolAssetTypeDto
import com.rarible.protocol.union.core.model.UnionTezosFTAssetTypeDto
import com.rarible.protocol.union.core.model.UnionTezosMTAssetTypeDto
import com.rarible.protocol.union.core.model.UnionTezosNFTAssetTypeDto
import com.rarible.protocol.union.core.model.UnionTezosXTZAssetTypeDto
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
        source: UnionAssetDto,
        data: EnrichmentAssetData = EnrichmentAssetData.empty()
    ): AssetDto {
        return AssetDto(
            type = convert(source.type, data),
            value = source.value
        )
    }

    fun convert(
        source: UnionAssetTypeDto,
        data: EnrichmentAssetData = EnrichmentAssetData.empty()
    ): AssetTypeDto {
        return when (source) {
            is UnionEthAmmNftAssetTypeDto -> convert(source, data)
            is UnionEthCollectionAssetTypeDto -> convert(source, data)
            is UnionEthCryptoPunksAssetTypeDto -> convert(source, data)
            is UnionEthErc1155AssetTypeDto -> convert(source, data)
            is UnionEthErc1155LazyAssetTypeDto -> convert(source, data)
            is UnionEthErc20AssetTypeDto -> convert(source, data)
            is UnionEthErc721AssetTypeDto -> convert(source, data)
            is UnionEthErc721LazyAssetTypeDto -> convert(source, data)
            is UnionEthEthereumAssetTypeDto -> convert(source, data)
            is UnionEthGenerativeArtAssetTypeDto -> convert(source, data)
            is UnionFlowAssetTypeFtDto -> convert(source, data)
            is UnionFlowAssetTypeNftDto -> convert(source, data)
            is UnionSolanaFtAssetTypeDto -> convert(source, data)
            is UnionSolanaNftAssetTypeDto -> convert(source, data)
            is UnionSolanaSolAssetTypeDto -> convert(source, data)
            is UnionTezosFTAssetTypeDto -> convert(source, data)
            is UnionTezosMTAssetTypeDto -> convert(source, data)
            is UnionTezosNFTAssetTypeDto -> convert(source, data)
            is UnionTezosXTZAssetTypeDto -> convert(source, data)
        }
    }

    private fun convert(
        source: UnionEthAmmNftAssetTypeDto,
        data: EnrichmentAssetData
    ): EthAmmNftAssetTypeDto {
        return EthAmmNftAssetTypeDto(
            contract = source.contract,
            collection = source.getEnrichedCollection(data)
        )
    }

    private fun convert(
        source: UnionEthCollectionAssetTypeDto,
        data: EnrichmentAssetData
    ): EthCollectionAssetTypeDto {
        return EthCollectionAssetTypeDto(
            contract = source.contract,
            collection = source.getEnrichedCollection(data)
        )
    }

    private fun convert(
        source: UnionEthCryptoPunksAssetTypeDto,
        data: EnrichmentAssetData
    ): EthCryptoPunksAssetTypeDto {
        return EthCryptoPunksAssetTypeDto(
            contract = source.contract,
            tokenId = source.tokenId,
            collection = source.getEnrichedCollection(data),
        )
    }

    private fun convert(
        source: UnionEthErc1155AssetTypeDto,
        data: EnrichmentAssetData
    ): EthErc1155AssetTypeDto {
        return EthErc1155AssetTypeDto(
            contract = source.contract,
            tokenId = source.tokenId,
            collection = source.getEnrichedCollection(data),
        )
    }

    private fun convert(
        source: UnionEthErc1155LazyAssetTypeDto,
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
        source: UnionEthErc20AssetTypeDto,
        data: EnrichmentAssetData
    ): EthErc20AssetTypeDto {
        return EthErc20AssetTypeDto(
            contract = source.contract
        )
    }

    private fun convert(
        source: UnionEthErc721AssetTypeDto,
        data: EnrichmentAssetData
    ): EthErc721AssetTypeDto {
        return EthErc721AssetTypeDto(
            contract = source.contract,
            tokenId = source.tokenId,
            collection = source.getEnrichedCollection(data)
        )
    }

    private fun convert(
        source: UnionEthErc721LazyAssetTypeDto,
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
        source: UnionEthEthereumAssetTypeDto,
        data: EnrichmentAssetData
    ): EthEthereumAssetTypeDto {
        return EthEthereumAssetTypeDto()
    }

    private fun convert(
        source: UnionEthGenerativeArtAssetTypeDto,
        data: EnrichmentAssetData
    ): EthGenerativeArtAssetTypeDto {
        return EthGenerativeArtAssetTypeDto(
            contract = source.contract,
            collection = source.getEnrichedCollection(data)
        )
    }

    private fun convert(
        source: UnionFlowAssetTypeFtDto,
        data: EnrichmentAssetData
    ): FlowAssetTypeFtDto {
        return FlowAssetTypeFtDto(
            contract = source.contract
        )
    }

    private fun convert(
        source: UnionFlowAssetTypeNftDto,
        data: EnrichmentAssetData
    ): FlowAssetTypeNftDto {
        return FlowAssetTypeNftDto(
            contract = source.contract,
            tokenId = source.tokenId,
            collection = source.getEnrichedCollection(data)
        )
    }

    private fun convert(
        source: UnionSolanaFtAssetTypeDto,
        data: EnrichmentAssetData
    ): SolanaFtAssetTypeDto {
        return SolanaFtAssetTypeDto(
            address = source.address
        )
    }

    private fun convert(
        source: UnionSolanaNftAssetTypeDto,
        data: EnrichmentAssetData
    ): SolanaNftAssetTypeDto {
        return SolanaNftAssetTypeDto(
            contract = source.contract,
            itemId = source.itemId,
            collection = source.getEnrichedCollection(data)
        )
    }

    private fun convert(
        source: UnionSolanaSolAssetTypeDto,
        data: EnrichmentAssetData
    ): SolanaSolAssetTypeDto {
        return SolanaSolAssetTypeDto()
    }

    private fun convert(
        source: UnionTezosFTAssetTypeDto,
        data: EnrichmentAssetData
    ): TezosFTAssetTypeDto {
        return TezosFTAssetTypeDto(
            contract = source.contract,
            tokenId = source.tokenId
        )
    }

    private fun convert(
        source: UnionTezosMTAssetTypeDto,
        data: EnrichmentAssetData
    ): TezosMTAssetTypeDto {
        return TezosMTAssetTypeDto(
            contract = source.contract,
            tokenId = source.tokenId,
            collection = source.getEnrichedCollection(data)
        )
    }

    private fun convert(
        source: UnionTezosNFTAssetTypeDto,
        data: EnrichmentAssetData
    ): TezosNFTAssetTypeDto {
        return TezosNFTAssetTypeDto(
            contract = source.contract,
            tokenId = source.tokenId,
            collection = source.getEnrichedCollection(data)
        )
    }

    private fun convert(
        source: UnionTezosXTZAssetTypeDto,
        data: EnrichmentAssetData
    ): TezosXTZAssetTypeDto {
        return TezosXTZAssetTypeDto()
    }

    private fun UnionAssetTypeDto.getEnrichedCollection(data: EnrichmentAssetData): CollectionIdDto? {
        return data.customCollection ?: this.collectionId()
    }

}