package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.NftItemDeleteEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.EthItemDeleteEventDto
import com.rarible.protocol.union.dto.EthItemUpdateEventDto
import com.rarible.protocol.union.dto.UnionItemEventDto
import com.rarible.protocol.union.dto.parser.UnionItemIdParser

object EthUnionItemEventConverter {

    fun convert(source: NftItemEventDto, blockchain: BlockchainDto): UnionItemEventDto {
        val itemId = UnionItemIdParser.parseShort(source.itemId, blockchain)
        return when (source) {
            is NftItemUpdateEventDto -> EthItemUpdateEventDto(
                eventId = source.eventId,
                itemId = itemId,
                item = EthUnionItemConverter.convert(source.item, blockchain)
            )
            is NftItemDeleteEventDto -> EthItemDeleteEventDto(
                eventId = source.eventId,
                itemId = itemId
            )
        }
    }

}

