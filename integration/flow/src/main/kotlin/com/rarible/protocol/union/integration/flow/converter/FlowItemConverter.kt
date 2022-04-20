package com.rarible.protocol.union.integration.flow.converter

import com.rarible.protocol.dto.FlowCreatorDto
import com.rarible.protocol.dto.FlowNftItemDto
import com.rarible.protocol.dto.FlowNftItemsDto
import com.rarible.protocol.dto.FlowRoyaltyDto
import com.rarible.protocol.dto.PayInfoDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MetaAttributeDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.parser.IdParser
import java.math.BigInteger
import org.slf4j.LoggerFactory

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
            meta = item.meta?.let { convert(it) },
            deleted = item.deleted,
            creators = item.creators.map { convert(it, blockchain) },
            lazySupply = BigInteger.ZERO
        )
    }

    fun convert(page: FlowNftItemsDto, blockchain: BlockchainDto): Page<UnionItem> {
        return Page(
            total = page.total,
            continuation = page.continuation,
            entities = page.items.map { convert(it, blockchain) }
        )
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

    fun convert(source: com.rarible.protocol.dto.MetaDto): UnionMeta {
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
            content = source.contents.orEmpty().map { url ->
                UnionMetaContent(
                    url = url,
                    representation = MetaContentDto.Representation.ORIGINAL
                )
            },
            // TODO FLOW - implement it
            restrictions = emptyList()
        )
    }
}
