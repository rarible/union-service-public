package com.rarible.protocol.union.integration.flow.converter

import com.rarible.protocol.dto.FlowAudioContentDto
import com.rarible.protocol.dto.FlowCreatorDto
import com.rarible.protocol.dto.FlowHtmlContentDto
import com.rarible.protocol.dto.FlowImageContentDto
import com.rarible.protocol.dto.FlowMetaContentItemDto
import com.rarible.protocol.dto.FlowModel3dContentDto
import com.rarible.protocol.dto.FlowNftItemDeleteEventDto
import com.rarible.protocol.dto.FlowNftItemDto
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.dto.FlowNftItemUpdateEventDto
import com.rarible.protocol.dto.FlowNftItemsDto
import com.rarible.protocol.dto.FlowUnknownContentDto
import com.rarible.protocol.dto.FlowVideoContentDto
import com.rarible.protocol.dto.PayInfoDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.MetaSource
import com.rarible.protocol.union.core.model.UnionAudioProperties
import com.rarible.protocol.union.core.model.UnionHtmlProperties
import com.rarible.protocol.union.core.model.UnionImageProperties
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.core.model.UnionModel3dProperties
import com.rarible.protocol.union.core.model.UnionUnknownProperties
import com.rarible.protocol.union.core.model.UnionVideoProperties
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.parser.IdParser
import org.slf4j.LoggerFactory
import java.math.BigInteger

object FlowItemConverter {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun convert(item: FlowNftItemDto, blockchain: BlockchainDto): UnionItem {
        try {
            return convertInternal(item, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Item: {} \n{}", blockchain, e.message, item)
            throw e
        }
    }

    private fun convertInternal(item: FlowNftItemDto, blockchain: BlockchainDto): UnionItem {
        return UnionItem(
            id = ItemIdDto(
                blockchain = blockchain,
                contract = IdParser.split(item.id, 2).first(),
                tokenId = item.tokenId
            ),
            collection = CollectionIdDto(blockchain, item.collection),
            mintedAt = item.mintedAt,
            lastUpdatedAt = item.lastUpdatedAt,
            supply = item.supply,
            deleted = item.deleted,
            creators = item.creators.map { convert(it, blockchain) },
            lazySupply = BigInteger.ZERO
        )
    }

    fun convert(page: FlowNftItemsDto, blockchain: BlockchainDto): Page<UnionItem> {
        return Page(
            total = 0,
            continuation = page.continuation,
            entities = page.items.map { convert(it, blockchain) }
        )
    }

    fun convert(event: FlowNftItemEventDto, blockchain: BlockchainDto): UnionItemEvent {
        val marks = FlowConverter.convert(event.eventTimeMarks)
        return when (event) {
            is FlowNftItemUpdateEventDto -> {
                val item = convert(event.item, blockchain)
                UnionItemUpdateEvent(item, marks)
            }

            is FlowNftItemDeleteEventDto -> {
                val itemId = ItemIdDto(
                    blockchain = blockchain,
                    contract = event.item.token,
                    tokenId = event.item.tokenId.toBigInteger()
                )
                UnionItemDeleteEvent(itemId, marks)
            }
        }
    }

    private fun convert(
        source: FlowCreatorDto,
        blockchain: BlockchainDto
    ): CreatorDto {
        return CreatorDto(
            account = UnionAddressConverter.convert(blockchain, source.account),
            value = FlowConverter.toBasePoints(source.value)
        )
    }

    fun toRoyalty(
        source: PayInfoDto,
        blockchain: BlockchainDto
    ): RoyaltyDto {
        return RoyaltyDto(
            account = UnionAddressConverter.convert(blockchain, source.account),
            value = FlowConverter.toBasePoints(source.value)
        )
    }

    fun convert(source: com.rarible.protocol.dto.FlowMetaDto, itemId: String): UnionMeta {
        val content = source.content?.map(::convert) ?: emptyList()
        return UnionMeta(
            name = source.name,
            collectionId = itemId.split(":").first(),
            description = source.description,
            language = source.language,
            genres = source.genres ?: emptyList(),
            tags = source.tags ?: emptyList(),
            createdAt = source.createdAt,
            rights = source.rights,
            rightsUri = source.rightsUrl,
            externalUri = source.externalUri,
            originalMetaUri = source.originalMetaUri,
            attributes = source.attributes.orEmpty().map {
                UnionMetaAttribute(
                    key = it.key,
                    value = it.value,
                    type = it.type,
                    format = it.format
                )
            },
            content = content,
            // TODO FLOW - implement it
            restrictions = emptyList(),
            source = MetaSource.ORIGINAL,
        )
    }

    fun convert(source: String): UnionMetaContent =
        UnionMetaContent(
            url = source,
            representation = MetaContentDto.Representation.ORIGINAL
        )

    fun convert(source: FlowMetaContentItemDto): UnionMetaContent {
        val size = source.size?.toLong()

        val properties = when (source) {
            is FlowImageContentDto -> UnionImageProperties(
                mimeType = source.mimeType,
                size = size,
                width = source.width,
                height = source.height
            )

            is FlowVideoContentDto -> UnionVideoProperties(
                mimeType = source.mimeType,
                size = size,
                width = source.width,
                height = source.height
            )

            is FlowAudioContentDto -> UnionAudioProperties(
                mimeType = source.mimeType,
                size = size
            )

            is FlowModel3dContentDto -> UnionModel3dProperties(
                mimeType = source.mimeType,
                size = size
            )

            is FlowHtmlContentDto -> UnionHtmlProperties(
                mimeType = source.mimeType,
                size = size
            )

            is FlowUnknownContentDto -> UnionUnknownProperties(
                mimeType = source.mimeType,
                size = size
            )
        }
        return UnionMetaContent(
            url = source.url,
            representation = MetaContentDto.Representation.valueOf(source.representation.name),
            fileName = source.fileName,
            properties = properties
        )
    }
}
