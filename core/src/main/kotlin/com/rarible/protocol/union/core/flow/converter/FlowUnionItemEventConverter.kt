package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.FlowNftItemDeleteEventDto
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.dto.FlowNftItemUpdateEventDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.FlowItemDeleteEventDto
import com.rarible.protocol.union.dto.FlowItemUpdateEventDto
import com.rarible.protocol.union.dto.UnionItemEventDto
import com.rarible.protocol.union.dto.parser.UnionItemIdParser

object FlowUnionItemEventConverter {

    fun convert(source: FlowNftItemEventDto, blockchain: BlockchainDto): UnionItemEventDto {
        val itemId = UnionItemIdParser.parseShort(source.itemId, blockchain)
        return when (source) {
            is FlowNftItemUpdateEventDto -> FlowItemUpdateEventDto(
                eventId = source.eventId,
                itemId = itemId,
                item = FlowUnionItemConverter.convert(source.item, blockchain)
            )
            is FlowNftItemDeleteEventDto -> FlowItemDeleteEventDto(
                eventId = source.eventId,
                itemId = itemId
            )
        }
    }

}

