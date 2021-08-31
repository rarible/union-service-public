package com.rarible.protocol.union.core.converter.flow

import com.rarible.protocol.dto.FlowNftItemDeleteEventDto
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.dto.FlowNftItemUpdateEventDto
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.dto.serializer.flow.FlowItemIdParser
import org.springframework.core.convert.converter.Converter
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant

object FlowUnionItemEventDtoConverter : Converter<FlowNftItemEventDto, UnionItemEventDto> {

    override fun convert(source: FlowNftItemEventDto): UnionItemEventDto {
        val itemId = FlowItemIdParser.parseShort(source.itemId)
        return when (source) {
            is FlowNftItemUpdateEventDto -> FlowItemUpdateEventDto(
                eventId = source.eventId,
                itemId = itemId,
                item = FlowItemDto(
                    mintedAt = source.item.date ?: Instant.now(),
                    lastUpdatedAt = source.item.date ?: Instant.now(),
                    supply = BigInteger.ONE,
                    metaURL = null, //TODO
                    blockchain = FlowItemDto.Blockchain.FLOW,
                    meta = MetaDto(name = "", raw = source.item.meta), //TODO: Not full meta
                    deleted = false, //TODO: No needed filed
                    id = itemId,
                    tokenId = source.item.tokenId?.toBigInteger() ?: BigInteger.ZERO, //TODO: Must not be null
                    collection = FlowContract(source.item.contract ?: ""),
                    creators = listOf(
                        FlowCreatorDto(
                            FlowAddress(source.item.creator ?: ""),
                            BigDecimal.ONE
                        )
                    ), //TODO: Not suitable type
                    owners = listOf(FlowAddress(source.item.owner ?: "")), //TODO: Must not be null
                    royalties = emptyList() //TODO: Does Flow have royalties
                )
            )
            is FlowNftItemDeleteEventDto -> FlowItemDeleteEventDto(
                eventId = source.eventId,
                itemId = itemId
            )
        }
    }
}

