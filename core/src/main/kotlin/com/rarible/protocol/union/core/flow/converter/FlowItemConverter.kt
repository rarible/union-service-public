package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.FlowCreatorDto
import com.rarible.protocol.dto.FlowNftItemDto
import com.rarible.protocol.dto.FlowNftItemsDto
import com.rarible.protocol.dto.FlowRoyaltyDto
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.ImageContentDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.MetaAttributeDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.dto.MetaDto
import com.rarible.protocol.union.dto.RoyaltyDto
import com.rarible.protocol.union.dto.UnionItemDto
import com.rarible.protocol.union.dto.VideoContentDto
import java.math.BigInteger

object FlowItemConverter {

    fun convert(item: FlowNftItemDto, blockchain: BlockchainDto): UnionItemDto {
        val collection = UnionAddressConverter.convert(item.collection, blockchain)

        return UnionItemDto(
            id = ItemIdDto(
                blockchain = blockchain,
                token = collection,
                tokenId = item.tokenId
            ),
            mintedAt = item.mintedAt,
            lastUpdatedAt = item.lastUpdatedAt,
            supply = item.supply,
            meta = item.meta?.let { convert(it) },
            deleted = item.deleted,
            tokenId = item.tokenId,
            collection = collection,
            creators = item.creators.map { convert(it, blockchain) },
            owners = item.owners.map { UnionAddressConverter.convert(it, blockchain) },
            royalties = item.royalties.map { convert(it, blockchain) },
            lazySupply = BigInteger.ZERO
        )
    }

    fun convert(page: FlowNftItemsDto, blockchain: BlockchainDto): Page<UnionItemDto> {
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
            account = UnionAddressConverter.convert(source.account, blockchain),
            value = source.value
        )
    }

    private fun convert(
        source: FlowRoyaltyDto,
        blockchain: BlockchainDto
    ): RoyaltyDto {
        return RoyaltyDto(
            account = UnionAddressConverter.convert(source.account, blockchain),
            value = source.value
        )
    }

    fun convert(source: com.rarible.protocol.dto.MetaDto): MetaDto {
        return MetaDto(
            name = source.name,
            description = source.description,
            attributes = source.attributes.orEmpty()
                .map {
                    MetaAttributeDto(
                        key = it.key,
                        value = it.value,
                        type = null,
                        format = null
                    )
                },
            // TODO FLOW improve MetaData model in order to identify video/image
            content = source.contents.orEmpty().map { dto ->
                if ("video" in dto.contentType) {
                    VideoContentDto(
                        url = dto.url,
                        representation = MetaContentDto.Representation.ORIGINAL,
                        mimeType = dto.contentType
                    )
                } else {
                    ImageContentDto(
                        url = dto.url,
                        representation = MetaContentDto.Representation.ORIGINAL,
                        mimeType = dto.contentType
                    )
                }
            },
            raw = source.raw
        )
    }
}
