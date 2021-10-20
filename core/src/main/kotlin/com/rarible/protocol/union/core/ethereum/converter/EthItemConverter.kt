package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.NftItemsDto
import com.rarible.protocol.dto.NftMediaDto
import com.rarible.protocol.dto.NftMediaMetaDto
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ImageContentDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemRoyaltyDto
import com.rarible.protocol.union.dto.ItemTransferDto
import com.rarible.protocol.union.dto.MetaAttributeDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.dto.MetaDto
import com.rarible.protocol.union.dto.UnionItemDto
import com.rarible.protocol.union.dto.VideoContentDto

object EthItemConverter {

    fun convert(item: NftItemDto, blockchain: BlockchainDto): UnionItemDto {
        return UnionItemDto(
            id = ItemIdDto(
                token = UnionAddressConverter.convert(item.contract, blockchain),
                tokenId = item.tokenId,
                blockchain = blockchain
            ),
            mintedAt = item.date ?: nowMillis(), // TODO ETHEREUM RPN-848
            lastUpdatedAt = item.date ?: nowMillis(),
            supply = item.supply,
            meta = item.meta?.let { convert(it) },
            deleted = item.deleted ?: false,
            tokenId = item.tokenId,
            collection = UnionAddressConverter.convert(item.contract, blockchain),
            creators = item.creators.map { EthConverter.convertToCreator(it, blockchain) },
            owners = item.owners.map { UnionAddressConverter.convert(it, blockchain) },
            royalties = item.royalties.map { EthConverter.convertToRoyalty(it, blockchain) },
            lazySupply = item.lazySupply,
            pending = item.pending?.map { convert(it, blockchain) } ?: listOf()
        )
    }

    fun convert(page: NftItemsDto, blockchain: BlockchainDto): Page<UnionItemDto> {
        return Page(
            total = page.total,
            continuation = page.continuation,
            entities = page.items.map { convert(it, blockchain) }
        )
    }

    fun convert(source: com.rarible.protocol.dto.ItemTransferDto, blockchain: BlockchainDto): ItemTransferDto {
        return ItemTransferDto(
            owner = UnionAddressConverter.convert(source.owner, blockchain),
            contract = UnionAddressConverter.convert(source.contract, blockchain),
            tokenId = source.tokenId,
            value = source.value,
            date = source.date,
            from = UnionAddressConverter.convert(source.from, blockchain)
        )
    }

    fun convert(source: com.rarible.protocol.dto.ItemRoyaltyDto, blockchain: BlockchainDto): ItemRoyaltyDto {
        return ItemRoyaltyDto(
            owner = source.owner?.let { UnionAddressConverter.convert(it, blockchain) },
            contract = UnionAddressConverter.convert(source.contract, blockchain),
            tokenId = source.tokenId,
            value = source.value!!,
            date = source.date,
            royalties = source.royalties.map { EthConverter.convertToRoyalty(it, blockchain) }
        )
    }

    fun convert(source: NftItemMetaDto): MetaDto {
        return MetaDto(
            name = source.name,
            description = source.description,
            attributes = source.attributes.orEmpty().map {
                MetaAttributeDto(
                    key = it.key,
                    value = it.value,
                    type = it.type,
                    format = it.format
                )
            },
            content = convertMetaContent(source.image, this::convertImage)
                    + convertMetaContent(source.animation, this::convertVideo),
            raw = null //TODO UNION Remove?
        )
    }

    private fun <T : MetaContentDto> convertMetaContent(
        source: NftMediaDto?, converter: (
            url: String,
            representation: MetaContentDto.Representation,
            meta: NftMediaMetaDto?
        ) -> T
    ): List<T> {
        return source?.url?.map { urlMap ->
            // TODO UNION handle unknown representation
            val representation = MetaContentDto.Representation.valueOf(urlMap.key)
            val url = urlMap.value
            val meta = source.meta[urlMap.key]
            converter(url, representation, meta)
        } ?: emptyList()
    }

    private fun convertImage(
        url: String,
        representation: MetaContentDto.Representation,
        meta: NftMediaMetaDto?
    ): ImageContentDto {
        return ImageContentDto(
            representation = representation,
            url = url,
            mimeType = meta?.type,
            width = meta?.width,
            height = meta?.height,
            size = null // Not available.
        )
    }

    private fun convertVideo(
        url: String,
        representation: MetaContentDto.Representation,
        meta: NftMediaMetaDto?
    ): VideoContentDto {
        return VideoContentDto(
            representation = representation,
            url = url,
            mimeType = meta?.type,
            width = meta?.width,
            height = meta?.height,
            size = null // Not available.
        )
    }
}

