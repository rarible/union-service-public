package com.rarible.protocol.union.integration.tezos.converter

import com.rarible.protocol.tezos.dto.NftItemAttributeDto
import com.rarible.protocol.tezos.dto.NftItemDto
import com.rarible.protocol.tezos.dto.NftItemMetaDto
import com.rarible.protocol.tezos.dto.NftItemsDto
import com.rarible.protocol.tezos.dto.PartDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MetaAttributeDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.continuation.page.Page
import org.slf4j.LoggerFactory
import java.time.Instant

object TezosItemConverter {

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
        return UnionItem(
            id = ItemIdDto(
                blockchain = blockchain,
                contract = item.contract,
                tokenId = item.tokenId
            ),
            collection = CollectionIdDto(blockchain, item.contract), // For TEZOS collection is a contract value
            creators = item.creators.map { TezosConverter.convertToCreator(it, blockchain) },
            deleted = item.deleted,
            lastUpdatedAt = item.date,
            lazySupply = item.lazySupply,
            meta = item.meta?.let { convert(it) },
            mintedAt = item.mintedAt,
            owners = emptyList(),
            // TODO TEZOS Remove when Tezos implement getItemRoyalties
            royalties = item.royalties.map { toRoyalty(it, blockchain) },
            supply = item.supply,
            pending = emptyList() // In Union we won't use this field for Tezos
        )
    }

    fun convert(page: NftItemsDto, blockchain: BlockchainDto): Page<UnionItem> {
        return Page(
            total = page.total.toLong(),
            continuation = page.continuation,
            entities = page.items.map { convert(it, blockchain) }
        )
    }

    fun convert(meta: NftItemMetaDto): UnionMeta =
        UnionMeta(
            name = meta.name,
            description = meta.description,
            attributes = meta.attributes.orEmpty().map(::convert),
            content = listOfNotNull(
                meta.image?.let { UnionMetaContent(it, MetaContentDto.Representation.ORIGINAL) },
                meta.animation?.let { UnionMetaContent(it, MetaContentDto.Representation.ORIGINAL) }
            ),
            // TODO TEZOS - implement it
            restrictions = listOf()
        )

    fun convert(attr: NftItemAttributeDto) = when {
        attr.type == "date" && attr.value.toLongOrNull() != null -> MetaAttributeDto(
            key = attr.key,
            value = Instant.ofEpochMilli(attr.value.safeMs()).toString(),
            type = "string",
            format = "date-time"
        )
        else -> MetaAttributeDto(
            key = attr.key,
            value = attr.value,
            type = attr.type,
            format = attr.format
        )
    }

    fun toRoyalty(
        source: PartDto,
        blockchain: BlockchainDto
    ): RoyaltyDto {
        return RoyaltyDto(
            account = UnionAddressConverter.convert(blockchain, source.account),
            value = source.value
        )
    }

    private fun String.safeMs(): Long {
        val ms = this.toLong()
        return when {
            ms <= Int.MAX_VALUE -> ms * 1000
            else -> ms
        }
    }
}
