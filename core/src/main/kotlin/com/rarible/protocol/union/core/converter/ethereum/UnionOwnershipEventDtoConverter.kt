package com.rarible.protocol.union.core.converter.ethereum

import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.*
import com.rarible.protocol.union.dto.*

object UnionOwnershipEventDtoConverter {
    fun convert(source: NftOwnershipEventDto, blockchain: Blockchain): UnionOwnershipEventDto {
        return when (source) {
            is NftOwnershipUpdateEventDto -> {
                UnionOwnershipUpdateEventDto(
                    eventId = source.eventId,
                    ownershipId = OwnershipId(source.ownershipId),
                    ownership = EthOwnershipDto(
                        //TODO: Need blockchain field
                        value = source.ownership.value,
                        createdAt = source.ownership.date,
                        id  = EthOwnershipId(source.ownership.id),
                        contract = EthAddress(source.ownership.contract.prefixed()),
                        tokenId = source.ownership.tokenId,
                        owner = listOf(EthAddressConverter.convert(source.ownership.owner)),
                        creators = source.ownership.creators.map { EthCreatorDtoConverter.convert(it) },
                        lazyValue = source.ownership.lazyValue,
                        pending = source.ownership.pending.mapNotNull { convert(it) }
                    )
                )
            }
            is NftOwnershipDeleteEventDto -> {
                UnionOwnershipDeleteEventDto(
                    eventId = source.eventId,
                    ownershipId = OwnershipId(source.ownershipId)
                )
            }
        }
    }

    private fun convert(source: ItemHistoryDto): EthPendingOwnershipDto? {
        return when (source) {
            is ItemRoyaltyDto -> EthPendingOwnershipRoyaltyDto(
                //TODO: Not full object
                from = source.royalties.map { EthRoyaltyDtoConverter.convert(it) }
            )
            is ItemTransferDto -> EthPendingOwnershipTransferDto(
                //TODO: Not full object
                from = EthAddressConverter.convert(source.from)
            )
        }
    }
}
