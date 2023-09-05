package com.rarible.protocol.union.integration.tezos.dipdup.converter

import com.rarible.dipdup.client.core.model.DipDupItem
import com.rarible.dipdup.client.core.model.DipDupRoyalties
import com.rarible.dipdup.client.core.model.Part
import com.rarible.dipdup.client.core.model.TokenMeta
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.MetaSource
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.continuation.page.Page
import org.slf4j.LoggerFactory
import java.math.BigInteger

object DipDupItemConverter {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val blockchain = BlockchainDto.TEZOS

    fun convert(source: DipDupItem): UnionItem {
        try {
            return convertInternal(source, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Collection: {} \n{}", blockchain, e.message, source)
            throw e
        }
    }

    fun convert(tzktPage: com.rarible.tzkt.model.Page<DipDupItem>, blockchain: BlockchainDto): Page<UnionItem> {
        return Page(
            total = tzktPage.items.size.toLong(),
            continuation = tzktPage.continuation,
            entities = tzktPage.items.map { convertInternal(it, blockchain) }
        )
    }

    fun convertMeta(source: TokenMeta): UnionMeta {
        try {
            return convertInternal(source)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Meta: {} \n{}", blockchain, e.message, source)
            throw e
        }
    }

    fun convert(source: DipDupRoyalties): List<RoyaltyDto> {
        try {
            return source.parts.map { toRoyalty(it, blockchain) }
        } catch (e: Exception) {
            logger.error("Failed to convert {} Royalty: {} \n{}", blockchain, e.message, source)
            throw e
        }
    }

    private fun toRoyalty(source: Part, blockchain: BlockchainDto): RoyaltyDto {
        return RoyaltyDto(
            account = UnionAddressConverter.convert(blockchain, source.account),
            value = source.value
        )
    }

    private fun toCreators(source: List<Part>, blockchain: BlockchainDto): List<CreatorDto> {
        return source.map { CreatorDto(UnionAddressConverter.convert(blockchain, it.account), it.value) }
    }

    private fun convertInternal(meta: TokenMeta): UnionMeta {
        return UnionMeta(
            name = meta.name,
            description = meta.description,
            attributes = meta.attributes.map(::convert),
            tags = meta.tags,
            content = meta.content.map(::convert),
            // TODO TEZOS - implement it
            restrictions = listOf(),
            source = MetaSource.ORIGINAL,
        )
    }

    private fun convert(content: TokenMeta.Content): UnionMetaContent {
        val representation = when (content.representation) {
            TokenMeta.Representation.ORIGINAL -> MetaContentDto.Representation.ORIGINAL
            TokenMeta.Representation.BIG -> MetaContentDto.Representation.BIG
            TokenMeta.Representation.PREVIEW -> MetaContentDto.Representation.PREVIEW
        }
        return UnionMetaContent(
            url = content.uri ?: "", // sometimes people don't fill meta properly
            representation = representation,
            properties = null
        )
    }

    private fun convert(attr: TokenMeta.Attribute): UnionMetaAttribute {
        return UnionMetaAttribute(
            key = attr.key,
            value = attr.value,
            type = attr.type,
            format = attr.format
        )
    }

    private fun convertInternal(item: DipDupItem, blockchain: BlockchainDto): UnionItem {
        return UnionItem(
            id = ItemIdDto(
                blockchain = blockchain,
                value = item.id
            ),
            collection = CollectionIdDto(
                blockchain,
                item.contract
            ), // For TEZOS collection is a contract value
            creators = toCreators(item.creators, blockchain),
            deleted = item.deleted,
            lastUpdatedAt = item.updated,
            lazySupply = BigInteger.ZERO,
            meta = null,
            mintedAt = item.mintedAt,
            supply = item.supply,
            pending = emptyList() // In Union we won't use this field for Tezos
        )
    }
}
