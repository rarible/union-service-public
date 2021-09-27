package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.*
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.dto.UnionCreatorDto
import com.rarible.protocol.union.dto.UnionRoyaltyDto

object FlowUnionItemConverter {

    fun convert(item: FlowNftItemDto, blockchain: BlockchainDto): FlowItemDto {
        val collection = FlowContractConverter.convert(item.collection, blockchain)

        return FlowItemDto(
            id = UnionItemIdDto(
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
            royalties = item.royalties.map { convert(it, blockchain) }
        )
    }

    fun convert(page: FlowNftItemsDto, blockchain: BlockchainDto): UnionItemsDto {
        return UnionItemsDto(
            total = page.total!!.toLong(), // TODO should be required
            continuation = page.continuation,
            items = page.items.map { convert(it, blockchain) }
        )
    }

    private fun convert(
        source: FlowCreatorDto,
        blockchain: BlockchainDto
    ): UnionCreatorDto {
        return UnionCreatorDto(
            account = UnionAddressConverter.convert(source.account, blockchain),
            value = source.value
        )
    }

    private fun convert(
        source: FlowRoyaltyDto,
        blockchain: BlockchainDto
    ): UnionRoyaltyDto {
        return UnionRoyaltyDto(
            account = UnionAddressConverter.convert(source.account, blockchain),
            value = source.value
        )
    }

    private fun convert(source: MetaDto?): UnionMetaDto? {
        if (source == null) {
            return null
        }
        return UnionMetaDto(
            name = source.name,
            description = source.description,
            attributes = source.attributes?.map { convert(it) } ?: listOf(),
            contents = source.contents?.map { convert(it) } ?: listOf(),
            raw = source.raw
        )
    }

    private fun convert(source: MetaAttributeDto): UnionMetaAttributeDto {
        return UnionMetaAttributeDto(
            key = source.key,
            value = source.value
        )
    }

    private fun convert(source: MetaContentDto): UnionMetaContentDto {
        return UnionMetaContentDto(
            typeContent = source.contentType,
            url = source.url,
            attributes = source.attributes?.map { convert(it) } ?: listOf()
        )
    }
}
