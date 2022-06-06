package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.core.common.nowMillis
import com.rarible.protocol.dto.AudioContentDto
import com.rarible.protocol.dto.HtmlContentDto
import com.rarible.protocol.dto.ImageContentDto
import com.rarible.protocol.dto.Model3dContentDto
import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.NftItemsDto
import com.rarible.protocol.dto.NftMediaDto
import com.rarible.protocol.dto.NftMediaMetaDto
import com.rarible.protocol.dto.UnknownContentDto
import com.rarible.protocol.dto.VideoContentDto
import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.model.UnionAudioProperties
import com.rarible.protocol.union.core.model.UnionHtmlProperties
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionMetaContentProperties
import com.rarible.protocol.union.core.model.UnionModel3dProperties
import com.rarible.protocol.union.core.model.UnionUnknownProperties
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
            lazySupply = item.lazySupply,
            pending = item.pending?.map { convert(it, blockchain) } ?: listOf()
        )
    }

    fun convert(page: NftItemsDto, blockchain: BlockchainDto): Page<UnionItem> {
        return Page(
            total = page.total ?: 0,
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
        // Legacy format of Eth meta, should not be used
        val legacyContent = getLegacyContent(source)
        val modernContent = source.content?.map { convert(it) } ?: emptyList()
        val content = modernContent.ifEmpty { legacyContent }
        return UnionMeta(
            name = source.name,
            description = source.description,
            language = source.language,
            genres = source.genres ?: emptyList(),
            tags = source.tags ?: emptyList(),
            createdAt = source.createdAt,
            rights = source.rights,
            rightsUri = source.rightsUri,
            externalUri = source.externalUri,
            attributes = source.attributes.orEmpty().map {
                MetaAttributeDto(
                    key = it.key,
                    value = it.value,
                    type = it.type,
                    format = it.format
                )
            },
            content = content,
            // TODO deprecated, remove later
            restrictions = emptyList()
        )
    }

    fun convert(source: com.rarible.protocol.dto.MetaContentDto): UnionMetaContent {
        val properties = when (source) {
            is ImageContentDto -> UnionImageProperties(
                mimeType = source.mimeType,
                size = source.size,
                width = source.width,
                height = source.height
            )
            is VideoContentDto -> UnionVideoProperties(
                mimeType = source.mimeType,
                size = source.size,
                width = source.width,
                height = source.height
            )
            is AudioContentDto -> UnionAudioProperties(
                mimeType = source.mimeType,
                size = source.size
            )
            is Model3dContentDto -> UnionModel3dProperties(
                mimeType = source.mimeType,
                size = source.size
            )
            is HtmlContentDto -> UnionHtmlProperties(
                mimeType = source.mimeType,
                size = source.size
            )
            is UnknownContentDto -> UnionUnknownProperties(
                mimeType = source.mimeType,
                size = source.size
            )
        }
        return UnionMetaContent(
            url = source.url,
            // TODO UNION handle unknown representation
            fileName = source.fileName,
            representation = MetaContentDto.Representation.valueOf(source.representation.name),
            properties = properties
        )
    }

    @Deprecated("Should be removed")
    private fun getLegacyContent(source: NftItemMetaDto): List<UnionMetaContent> {
        return convertMetaContent(source.image) { imageMetaDto ->
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
        }
    }

    @Deprecated("Should be removed")
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
