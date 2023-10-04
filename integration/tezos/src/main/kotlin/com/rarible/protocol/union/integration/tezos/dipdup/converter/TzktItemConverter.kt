package com.rarible.protocol.union.integration.tezos.dipdup.converter

import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.MetaSource
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.tzkt.model.Part
import com.rarible.tzkt.model.Token
import com.rarible.tzkt.model.TokenMeta
import org.slf4j.LoggerFactory
import java.math.BigInteger

object TzktItemConverter {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun convert(source: Token, blockchain: BlockchainDto): UnionItem {
        try {
            return convertInternal(source, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Collection: {} \n{}", blockchain, e.message, source)
            throw e
        }
    }

    fun convert(tzktPage: com.rarible.tzkt.model.Page<Token>, blockchain: BlockchainDto): Page<UnionItem> {
        return Page(
            total = tzktPage.items.size.toLong(),
            continuation = tzktPage.continuation,
            entities = tzktPage.items.map { convertInternal(it, blockchain) }
        )
    }

    fun convert(source: TokenMeta, blockchain: BlockchainDto): UnionMeta {
        try {
            return convertInternal(source)
        } catch (e: Exception) {
            logger.error("Failed to convert {} Meta: {} \n{}", blockchain, e.message, source)
            throw e
        }
    }

    fun convert(source: List<Part>, blockchain: BlockchainDto): List<RoyaltyDto> {
        try {
            return source.map { toRoyalty(it, blockchain) }
        } catch (e: Exception) {
            logger.error("Failed to convert {} Royalty: {} \n{}", blockchain, e.message, source)
            throw e
        }
    }

    private fun toRoyalty(source: Part, blockchain: BlockchainDto): RoyaltyDto {
        return RoyaltyDto(
            account = UnionAddressConverter.convert(blockchain, source.address),
            value = source.share
        )
    }

    private fun convertInternal(meta: TokenMeta): UnionMeta {
        return UnionMeta(
            name = meta.name,
            description = meta.description,
            attributes = meta.attributes.map(::convert),
            tags = meta.tags,
            content = meta.content.map(::convert),
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

    private fun convertInternal(item: Token, blockchain: BlockchainDto): UnionItem {
        return UnionItem(
            id = ItemIdDto(
                blockchain = blockchain,
                contract = item.contract!!.address,
                tokenId = BigInteger(item.tokenId!!)
            ),
            collection = CollectionIdDto(
                blockchain,
                item.contract!!.address
            ), // For TEZOS collection is a contract value
            creators = emptyList(),
            deleted = item.isDeleted(),
            lastUpdatedAt = item.lastTime!!.toInstant(),
            lazySupply = BigInteger.ZERO,
            meta = item.meta?.let { convertInternal(it) },
            mintedAt = item.firstTime!!.toInstant(),
            supply = BigInteger(item.totalSupply),
            pending = emptyList() // In Union we won't use this field for Tezos
        )
    }
}
