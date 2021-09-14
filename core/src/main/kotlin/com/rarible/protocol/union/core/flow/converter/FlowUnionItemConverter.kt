package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.*
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.dto.FlowCreatorDto
import com.rarible.protocol.union.dto.FlowRoyaltyDto
import com.rarible.protocol.union.dto.flow.FlowItemIdProvider

object FlowUnionItemConverter {

    fun convert(item: FlowNftItemDto, blockchain: FlowBlockchainDto): FlowItemDto {
        val collection = FlowContractConverter.convert(item.collection, blockchain)

        return FlowItemDto(
            id = FlowItemIdProvider.create(collection, item.tokenId, blockchain),
            mintedAt = item.mintedAt,
            lastUpdatedAt = item.lastUpdatedAt,
            supply = item.supply,
            metaUrl = item.metaUrl,
            meta = convert(item.meta),
            deleted = item.deleted,
            tokenId = item.tokenId,
            collection = collection,
            creators = item.creators.map { convert(it, blockchain) },
            owners = item.owners.map { FlowAddressConverter.convert(it, blockchain) },
            royalties = item.royalties.map { convert(it, blockchain) }
        )
    }

    fun convert(page: FlowNftItemsDto, blockchain: FlowBlockchainDto): UnionItemsDto {
        return UnionItemsDto(
            total = page.total!!.toLong(), // TODO should be required
            continuation = page.continuation,
            items = page.items.map { convert(it, blockchain) }
        )
    }

    private fun convert(
        source: com.rarible.protocol.dto.FlowCreatorDto,
        blockchain: FlowBlockchainDto
    ): FlowCreatorDto {
        return FlowCreatorDto(
            account = FlowAddressConverter.convert(source.account, blockchain),
            value = source.value
        )
    }

    private fun convert(
        source: com.rarible.protocol.dto.FlowRoyaltyDto,
        blockchain: FlowBlockchainDto
    ): FlowRoyaltyDto {
        return FlowRoyaltyDto(
            account = FlowAddressConverter.convert(source.account, blockchain),
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
            raw = source.raw.toString() // TODO
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
