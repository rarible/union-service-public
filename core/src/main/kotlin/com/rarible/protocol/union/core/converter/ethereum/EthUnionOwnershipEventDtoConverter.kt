package com.rarible.protocol.union.core.converter.ethereum

import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.dto.*
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.dto.serializer.eth.EthOwnershipIdParser

object EthUnionOwnershipEventDtoConverter {

    fun convert(source: NftOwnershipEventDto, blockchain: Blockchain): UnionOwnershipEventDto {
        val ownershipId = EthOwnershipIdParser.parseShort(source.ownershipId)
        return when (source) {
            is NftOwnershipUpdateEventDto -> {
                EthOwnershipUpdateEventDto(
                    eventId = source.eventId,
                    ownershipId = ownershipId,
                    ownership = EthOwnershipDto(
                        value = source.ownership.value,
                        blockchain = EthBlockchainConverter.convert(blockchain),
                        createdAt = source.ownership.date,
                        id = ownershipId,
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
                EthOwnershipDeleteEventDto(
                    eventId = source.eventId,
                    ownershipId = ownershipId
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
