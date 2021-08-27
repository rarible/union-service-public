package com.rarible.protocol.union.core.converter.flow

import com.rarible.protocol.dto.*
import com.rarible.protocol.union.dto.*
import org.springframework.core.convert.converter.Converter
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

object FlowUnionItemEventDtoConverter : Converter<FlowNftItemEventDto, UnionItemEventDto> {
    override fun convert(source: FlowNftItemEventDto): UnionItemEventDto {
        return when (source) {
            is FlowNftItemUpdateEventDto -> UnionItemUpdateEventDto(
                eventId = source.eventId,
                itemId = ItemId(source.itemId),
                item = FlowItemDto(
                    mintedAt = source.item.date ?: Instant.now(),
                    lastUpdatedAt = source.item.date ?: Instant.now(),
                    supply = BigInteger.ONE,
                    metaURL = null, //TODO
                    blockchain  = FlowItemDto.Blockchain.FLOW,
                    meta = MetaDto(name = "", raw = source.item.meta), //TODO: Not full meta
                    deleted = false, //TODO: No needed filed
                    id = FlowItemId(source.item.id ?: ""), //TODO: Must not be null
                    tokenId = source.item.tokenId?.toBigInteger() ?: BigInteger.ZERO, //TODO: Must not be null
                    collection  = FlowContract(source.item.contract ?: ""),
                    creators = listOf(FlowCreatorDto(FlowAddress(source.item.creator ?: ""), BigDecimal.ONE)), //TODO: Not suitable type
                    owners = listOf(FlowAddress(source.item.owner ?: "")), //TODO: Must not be null
                    royalties = emptyList() //TODO: Does Flow have royalties
                )
            )
            is FlowNftItemDeleteEventDto -> UnionItemDeleteEventDto(
                eventId = source.eventId,
                itemId = ItemId(source.eventId) //TODO: Need typed itemId
            )
        }
    }
}

