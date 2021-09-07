package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.NftItemDeleteEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.union.dto.EthBlockchainDto
import com.rarible.protocol.union.dto.EthItemDeleteEventDto
import com.rarible.protocol.union.dto.EthItemUpdateEventDto
import com.rarible.protocol.union.dto.UnionItemEventDto
import com.rarible.protocol.union.dto.ethereum.EthItemIdProvider

object EthUnionItemEventConverter {

    fun convert(source: NftItemEventDto, blockchain: EthBlockchainDto): UnionItemEventDto {
        val itemId = EthItemIdProvider.parseShort(source.itemId, blockchain)
        return when (source) {
            is NftItemUpdateEventDto -> EthItemUpdateEventDto(
                eventId = source.eventId,
                itemId = itemId,
                item = EthUnionItemConverter.convert(source.item, blockchain)
            )
            is NftItemDeleteEventDto -> EthItemDeleteEventDto(
                eventId = source.eventId,
                itemId = itemId //TODO: Need typed itemId
            )
        }
    }

}

