package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.NftItemsDto
import com.rarible.protocol.dto.NftMediaDto
import com.rarible.protocol.dto.NftMediaMetaDto
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionMetaContentProperties
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemRoyaltyDto
import com.rarible.protocol.union.dto.ItemTransferDto
import com.rarible.protocol.union.dto.MetaAttributeDto
import com.rarible.protocol.union.dto.MetaContentDto

object EthItemConverter {

    fun convert(item: NftItemDto, blockchain: BlockchainDto): UnionItem {
        return UnionItem(
            id = ItemIdDto(
                contract = EthConverter.convert(item.contract),
                tokenId = item.tokenId,
                blockchain = blockchain
            ),
            mintedAt = item.date ?: nowMillis(), // TODO ETHEREUM RPN-848
            lastUpdatedAt = item.date ?: nowMillis(),
            supply = item.supply,
            meta = item.meta?.let { convert(it) },
            deleted = item.deleted ?: false,
            creators = item.creators.map { EthConverter.convertToCreator(it, blockchain) },
            // TODO UNION Remove in 1.19
            owners = emptyList(),
            // TODO UNION Remove in 1.19
            royalties = item.royalties.map { EthConverter.convertToRoyalty(it, blockchain) },
            lazySupply = item.lazySupply,
            pending = item.pending?.map { convert(it, blockchain) } ?: listOf()
        )
    }

    fun convert(page: NftItemsDto, blockchain: BlockchainDto): Page<UnionItem> {
        return Page(
            total = page.total,
            continuation = page.continuation,
            entities = page.items.map { convert(it, blockchain) }
        )
    }

    fun convert(source: com.rarible.protocol.dto.ItemTransferDto, blockchain: BlockchainDto): ItemTransferDto {
        return ItemTransferDto(
            owner = EthConverter.convert(source.owner, blockchain),
            contract = ContractAddress(blockchain, EthConverter.convert(source.contract)),
            tokenId = source.tokenId,
            value = source.value,
            date = source.date,
            from = EthConverter.convert(source.from, blockchain)
        )
    }

    fun convert(source: com.rarible.protocol.dto.ItemRoyaltyDto, blockchain: BlockchainDto): ItemRoyaltyDto {
        return ItemRoyaltyDto(
            owner = source.owner?.let { EthConverter.convert(it, blockchain) },
            contract = ContractAddress(blockchain, EthConverter.convert(source.contract)),
            tokenId = source.tokenId,
            value = source.value!!,
            date = source.date,
            royalties = source.royalties.map { EthConverter.convertToRoyalty(it, blockchain) }
        )
    }

    fun convert(source: NftItemMetaDto): UnionMeta {
        return UnionMeta(
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
            content = convertMetaContent(source.image, this::getImageHint)
                    + convertMetaContent(source.animation, this::getVideoHint),
            // TODO ETHEREUM - implement it
            restrictions = emptyList()
        )
    }

    private fun convertMetaContent(
        source: NftMediaDto?, hintConverter: (
            meta: NftMediaMetaDto?
        ) -> UnionMetaContentProperties
    ): List<UnionMetaContent> {
        return source?.url?.map { urlMap ->
            val meta = source.meta[urlMap.key]
            UnionMetaContent(
                url = urlMap.value,
                // TODO UNION handle unknown representation
                representation = MetaContentDto.Representation.valueOf(urlMap.key),
                properties = hintConverter(meta)
            )
        } ?: emptyList()
    }

    private fun getImageHint(
        meta: NftMediaMetaDto?
    ): UnionImageProperties {
        return UnionImageProperties(
            mimeType = meta?.type,
            width = meta?.width,
            height = meta?.height
        )
    }

    private fun getVideoHint(
        meta: NftMediaMetaDto?
    ): UnionVideoProperties {
        return UnionVideoProperties(
            mimeType = meta?.type,
            width = meta?.width,
            height = meta?.height
        )
    }
}

