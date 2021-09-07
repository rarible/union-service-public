package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.ItemHistoryDto
import com.rarible.protocol.dto.ItemRoyaltyDto
import com.rarible.protocol.dto.ItemTransferDto
import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.dto.ethereum.EthOwnershipIdProvider

object EthUnionOwnershipConverter {

    fun convert(source: NftOwnershipDto, blockchain: EthBlockchainDto): EthOwnershipDto {
        return EthOwnershipDto(
            id = EthOwnershipIdProvider.create(
                token = EthAddressConverter.convert(source.contract, blockchain),
                tokenId = source.tokenId,
                owner = EthAddressConverter.convert(source.owner, blockchain),
                blockchain = blockchain
            ),
            value = source.value,
            createdAt = source.date,
            contract = EthAddressConverter.convert(source.contract, blockchain),
            tokenId = source.tokenId,
            owner = listOf(EthAddressConverter.convert(source.owner, blockchain)),
            creators = source.creators.map { EthConverter.convertToCreator(it, blockchain) },
            lazyValue = source.lazyValue,
            pending = source.pending.map { convert(it, blockchain) }
        )
    }

    private fun convert(source: ItemHistoryDto, blockchain: EthBlockchainDto): EthPendingOwnershipDto {
        return when (source) {
            is ItemRoyaltyDto -> EthPendingOwnershipRoyaltyDto(
                //TODO: Not full object
                from = source.royalties.map { EthConverter.convertToRoyalty(it, blockchain) }
            )
            is ItemTransferDto -> EthPendingOwnershipTransferDto(
                //TODO: Not full object
                from = EthAddressConverter.convert(source.from, blockchain)
            )
        }
    }
}
