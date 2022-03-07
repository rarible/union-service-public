package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.NftItemsDto
import com.rarible.protocol.dto.NftMediaDto
import com.rarible.protocol.dto.NftMediaMetaDto
import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionMetaContentProperties
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemRoyaltyDto
import com.rarible.protocol.union.dto.ItemTransferDto
import com.rarible.protocol.union.dto.MetaAttributeDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.dto.continuation.page.Page
import org.slf4j.LoggerFactory

object EthItemConverter {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun convert(item: NftItemDto, blockchain: BlockchainDto): UnionItem {
        try {
            return convertInternal(item, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Item: {} \n{}", blockchain, e.message, item)
            throw e
        }
    }

    private fun convertInternal(item: NftItemDto, blockchain: BlockchainDto): UnionItem {
        val contract = EthConverter.convert(item.contract)
        return UnionItem(
            id = ItemIdDto(
                contract = contract,
                tokenId = item.tokenId,
                blockchain = blockchain
            ),
            collection = CollectionIdDto(blockchain, contract), // For ETH collection is a contract value
            mintedAt = item.mintedAt ?: nowMillis(),
            lastUpdatedAt = item.lastUpdatedAt ?: nowMillis(),
            supply = item.supply,
            // TODO: see CHARLIE-158: at some point, we will ignore meta from blockchains, and always load meta on union service.
            meta = item.meta?.let { convert(it) },
            deleted = item.deleted ?: false,
            creators = item.creators.map { EthConverter.convertToCreator(it, blockchain) },
            // TODO UNION Remove in 1.19
            owners = emptyList(),
            // TODO UNION Remove in 1.19
            royalties = item.royalties?.map { EthConverter.convertToRoyalty(it, blockchain) } ?: emptyList(),
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
            contract = ContractAddressConverter.convert(blockchain, EthConverter.convert(source.contract)),
            tokenId = source.tokenId,
            value = source.value,
            date = source.date,
            from = EthConverter.convert(source.from, blockchain)
        )
    }

    fun convert(source: com.rarible.protocol.dto.ItemRoyaltyDto, blockchain: BlockchainDto): ItemRoyaltyDto {
        return ItemRoyaltyDto(
            owner = source.owner?.let { EthConverter.convert(it, blockchain) },
            contract = ContractAddressConverter.convert(blockchain, EthConverter.convert(source.contract)),
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
            content = convertMetaContent(source.image) { imageMetaDto ->
                UnionImageProperties(
                    mimeType = imageMetaDto?.type,
                    width = imageMetaDto?.width,
                    height = imageMetaDto?.height,
                    size = null // TODO ETHEREUM - get from ETH OpenAPI.
                )
            } + convertMetaContent(source.animation) { videoMetaDto ->
                UnionVideoProperties(
                    mimeType = videoMetaDto?.type,
                    width = videoMetaDto?.width,
                    height = videoMetaDto?.height,
                    size = null // TODO ETHEREUM - get from ETH OpenAPI.
                )
            },
            // TODO ETHEREUM - implement it
            restrictions = emptyList()
        )
    }

    private fun convertMetaContent(
        source: NftMediaDto?,
        converter: (meta: NftMediaMetaDto?) -> UnionMetaContentProperties
    ): List<UnionMetaContent> {
        source ?: return emptyList()
        return source.url.map { (representationType, url) ->
            val meta = source.meta[representationType]
            UnionMetaContent(
                url = url,
                // TODO UNION handle unknown representation
                representation = MetaContentDto.Representation.valueOf(representationType),
                properties = converter(meta)
            )
        }
    }

}
