package com.rarible.protocol.union.core.converter.flow

import com.rarible.protocol.dto.*
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.dto.FlowCreatorDto
import com.rarible.protocol.union.dto.FlowRoyaltyDto
import com.rarible.protocol.union.dto.serializer.flow.FlowItemIdParser
import org.springframework.core.convert.converter.Converter

object FlowUnionItemEventDtoConverter : Converter<FlowNftItemEventDto, UnionItemEventDto> {

    override fun convert(source: FlowNftItemEventDto): UnionItemEventDto {
        val itemId = FlowItemIdParser.parseShort(source.itemId)
        return when (source) {
            is FlowNftItemUpdateEventDto -> FlowItemUpdateEventDto(
                eventId = source.eventId,
                itemId = itemId,
                item = FlowItemDto(
                    mintedAt = source.item.mintedAt,
                    lastUpdatedAt = source.item.lastUpdatedAt,
                    supply = source.item.supply,
                    metaURL = source.item.metaUrl,
                    blockchain = FlowBlockchainDto.FLOW,
                    meta = convert(source.item.meta),
                    deleted = source.item.deleted,
                    id = itemId,
                    tokenId = source.item.tokenId,
                    collection = FlowContract(source.item.collection),
                    creators = source.item.creators.map { convert(it) },
                    owners = source.item.owners.map { FlowAddressConverter.convert(it) },
                    royalties = source.item.royalties.map { convert(it) }
                )
            )
            is FlowNftItemDeleteEventDto -> FlowItemDeleteEventDto(
                eventId = source.eventId,
                itemId = itemId
            )
        }
    }

    private fun convert(source: com.rarible.protocol.dto.FlowCreatorDto): FlowCreatorDto {
        return FlowCreatorDto(
            account = FlowAddressConverter.convert(source.account),
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
            attributes = source.attributes?.map { convert(it) },
            contents = source.contents?.map { convert(it) },
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
            attributes = source.attributes?.map { convert(it) }
        )
    }

    private fun convert(source: com.rarible.protocol.dto.FlowRoyaltyDto): FlowRoyaltyDto {
        return FlowRoyaltyDto(
            account = FlowAddressConverter.convert(source.account),
            value = source.value
        )
    }
}

