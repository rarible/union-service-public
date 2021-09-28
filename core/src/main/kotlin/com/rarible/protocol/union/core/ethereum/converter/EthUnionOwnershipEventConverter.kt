package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.NftOwnershipDeleteEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.NftOwnershipUpdateEventDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionOwnershipDeleteEventDto
import com.rarible.protocol.union.dto.UnionOwnershipUpdateEventDto
import com.rarible.protocol.union.dto.UnionOwnershipEventDto
import com.rarible.protocol.union.dto.parser.UnionOwnershipIdParser

object EthUnionOwnershipEventConverter {

    fun convert(source: NftOwnershipEventDto, blockchain: BlockchainDto): UnionOwnershipEventDto {
        val ownershipId = UnionOwnershipIdParser.parseShort(source.ownershipId, blockchain)
        return when (source) {
            is NftOwnershipUpdateEventDto -> {
                UnionOwnershipUpdateEventDto(
                    eventId = source.eventId,
                    ownershipId = ownershipId,
                    ownership = EthUnionOwnershipConverter.convert(source.ownership, blockchain)
                )
            }
            is NftOwnershipDeleteEventDto -> {
                UnionOwnershipDeleteEventDto(
                    eventId = source.eventId,
                    ownershipId = ownershipId
                )
            }
        }
    }
}
