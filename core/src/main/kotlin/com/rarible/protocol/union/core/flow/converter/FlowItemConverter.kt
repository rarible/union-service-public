package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.FlowCreatorDto
import com.rarible.protocol.dto.FlowNftItemDto
import com.rarible.protocol.dto.FlowNftItemsDto
import com.rarible.protocol.dto.FlowRoyaltyDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemsDto
import com.rarible.protocol.union.dto.MetaAttributeDto
import com.rarible.protocol.union.dto.MetaContentDto
import com.rarible.protocol.union.dto.MetaDto
import com.rarible.protocol.union.dto.RoyaltyDto
import java.math.BigInteger

object FlowItemConverter {

    fun convert(item: FlowNftItemDto, blockchain: BlockchainDto): ItemDto {
        val collection = FlowContractConverter.convert(item.collection, blockchain)

        return ItemDto(
            id = ItemIdDto(
                blockchain = blockchain,
                token = collection,
                tokenId = item.tokenId
            ),
            mintedAt = item.mintedAt,
            lastUpdatedAt = item.lastUpdatedAt,
            supply = item.supply,
            metaUrl = item.metaUrl,
            meta = convert(item.meta),
            deleted = item.deleted,
            tokenId = item.tokenId,
            collection = collection,
            creators = item.creators.map { convert(it, blockchain) },
            owners = item.owners.map { UnionAddressConverter.convert(it, blockchain) },
            royalties = item.royalties.map { convert(it, blockchain) },
            lazySupply = BigInteger.ZERO
        )
    }

    fun convert(page: FlowNftItemsDto, blockchain: BlockchainDto): ItemsDto {
        return ItemsDto(
            total = page.total,
            continuation = page.continuation,
            items = page.items.map { convert(it, blockchain) }
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

    private fun convert(source: com.rarible.protocol.dto.MetaDto?): MetaDto? {
        if (source == null) {
            return null
        }
        return MetaDto(
            name = source.name,
            description = source.description,
            attributes = source.attributes?.map { convert(it) } ?: listOf(),
            contents = source.contents?.map { convert(it) } ?: listOf(),
            raw = source.raw
        )
    }

    private fun convert(source: com.rarible.protocol.dto.MetaAttributeDto): MetaAttributeDto {
        return MetaAttributeDto(
            key = source.key,
            value = source.value
        )
    }

    private fun convert(source: com.rarible.protocol.dto.MetaContentDto): MetaContentDto {
        return MetaContentDto(
            typeContent = source.contentType,
            url = source.url,
            attributes = source.attributes?.map { convert(it) } ?: listOf()
        )
    }
}
