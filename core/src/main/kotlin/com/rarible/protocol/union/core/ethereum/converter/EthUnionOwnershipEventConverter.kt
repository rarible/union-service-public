package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.union.dto.EthBlockchainDto
import com.rarible.protocol.union.dto.EthOwnershipDeleteEventDto
import com.rarible.protocol.union.dto.EthOwnershipUpdateEventDto
import com.rarible.protocol.union.dto.UnionOwnershipEventDto
import com.rarible.protocol.union.dto.ethereum.parser.EthOwnershipIdParser

object EthUnionOwnershipEventConverter {

    fun convert(source: NftOwnershipEventDto, blockchain: EthBlockchainDto): UnionOwnershipEventDto {
        val ownershipId = EthOwnershipIdParser.parseShort(source.ownershipId, blockchain)
        return when (source) {
            is NftOwnershipUpdateEventDto -> {
                EthOwnershipUpdateEventDto(
                    eventId = source.eventId,
                    ownershipId = ownershipId,
                    ownership = EthUnionOwnershipConverter.convert(source.ownership, blockchain)
                )
            }
            is NftOwnershipDeleteEventDto -> {
                EthOwnershipDeleteEventDto(
                    eventId = source.eventId,
                    ownershipId = ownershipId
                )
            }
        }
    }
}
