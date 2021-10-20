package com.rarible.protocol.union.core.tezos.converter

import com.rarible.protocol.tezos.dto.NftItemDto
import com.rarible.protocol.tezos.dto.NftItemMetaDto
import com.rarible.protocol.tezos.dto.NftItemsDto
import com.rarible.protocol.tezos.dto.PartDto
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ImageContentDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MetaAttributeDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.dto.MetaDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.UnionItemDto
import com.rarible.protocol.union.dto.VideoContentDto

object TezosItemConverter {

    fun convert(item: NftItemDto, blockchain: BlockchainDto): UnionItemDto {
        return UnionItemDto(
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

    fun convert(page: NftItemsDto, blockchain: BlockchainDto): Page<UnionItemDto> {
        return Page(
            total = page.total.toLong(),
            continuation = page.continuation,
            entities = page.items.map { convert(it, blockchain) }
        )
    }

    fun convert(meta: NftItemMetaDto): MetaDto =
        MetaDto(
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
                meta.image?.let { convertImage(it) },
                meta.animation?.let { convertVideo(it) }
            )
        )

    private fun convertImage(imageUrl: String): ImageContentDto =
        ImageContentDto(
            url = imageUrl,
            representation = MetaContentDto.Representation.ORIGINAL
            // Other fields will be fetched by the enrichment level.
        )

    private fun convertVideo(imageUrl: String): VideoContentDto =
        VideoContentDto(
            url = imageUrl,
            representation = MetaContentDto.Representation.ORIGINAL
            // Other fields will be fetched by the enrichment level.
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
