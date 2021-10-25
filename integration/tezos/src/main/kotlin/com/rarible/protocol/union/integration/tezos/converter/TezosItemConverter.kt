package com.rarible.protocol.union.integration.tezos.converter

import com.rarible.protocol.tezos.dto.NftItemDto
import com.rarible.protocol.tezos.dto.NftItemMetaDto
import com.rarible.protocol.tezos.dto.NftItemsDto
import com.rarible.protocol.tezos.dto.PartDto
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionMeta
import com.rarible.protocol.union.core.model.UnionMetaContent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MetaAttributeDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.UnionAddress

object TezosItemConverter {

    fun convert(item: NftItemDto, blockchain: BlockchainDto): UnionItem {
        return UnionItem(
            id = ItemIdDto(
                blockchain = blockchain,
                token = UnionAddress(blockchain, item.contract),
                tokenId = item.tokenId
            ),
            tokenId = item.tokenId,
            collection = UnionAddress(blockchain, item.contract),
            creators = item.creators.map { TezosConverter.convertToCreator(it, blockchain) },
            deleted = item.deleted ?: false, //TODO TEZOS raise to tezos, should be required
            lastUpdatedAt = item.date,
            lazySupply = item.lazySupply,
            meta = item.meta?.let { convert(it) },
            mintedAt = item.date, // TODO TEZOS add mintedAt
            owners = item.owners.map { UnionAddress(blockchain, it) },
            royalties = item.royalties.map { toRoyalty(it, blockchain) },
            supply = item.supply,
            pending = emptyList() // TODO TEZOS in union we won't use this field
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
            attributes = meta.attributes.orEmpty().map {
                MetaAttributeDto(
                    key = it.key,
                    value = it.value,
                    type = it.type,
                    format = it.format
                )
            },
            content = listOfNotNull(
                meta.image?.let { UnionMetaContent(it, MetaContentDto.Representation.ORIGINAL) },
                meta.animation?.let { UnionMetaContent(it, MetaContentDto.Representation.ORIGINAL) }
            )
        )

    private fun toRoyalty(
        source: PartDto,
        blockchain: BlockchainDto
    ): RoyaltyDto {
        return RoyaltyDto(
            account = UnionAddressConverter.convert(source.account, blockchain),
            value = source.value.toBigDecimal() // TODO TEZOS do we need some multiplier here?
        )
    }
}
