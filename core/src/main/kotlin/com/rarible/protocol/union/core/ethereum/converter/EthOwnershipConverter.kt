package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.ItemRoyaltyDto
import com.rarible.protocol.dto.ItemTransferDto
import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.dto.NftOwnershipsDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemHistoryDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipsDto

object EthOwnershipConverter {

    fun convert(source: NftOwnershipDto, blockchain: BlockchainDto): OwnershipDto {
        return OwnershipDto(
            id = OwnershipIdDto(
                token = UnionAddressConverter.convert(source.contract, blockchain),
                tokenId = source.tokenId,
                owner = UnionAddressConverter.convert(source.owner, blockchain),
                blockchain = blockchain
            ),
            value = source.value,
            createdAt = source.date,
            contract = UnionAddressConverter.convert(source.contract, blockchain),
            tokenId = source.tokenId,
            owner = UnionAddressConverter.convert(source.owner, blockchain),
            creators = source.creators.map { EthConverter.convertToCreator(it, blockchain) },
            lazyValue = source.lazyValue,
            pending = source.pending.map { convert(it, blockchain) }
        )
    }

    fun convert(page: NftOwnershipsDto, blockchain: BlockchainDto): OwnershipsDto {
        return OwnershipsDto(
            total = page.total,
            continuation = page.continuation,
            ownerships = page.ownerships.map { convert(it, blockchain) }
        )
    }

    private fun convert(source: com.rarible.protocol.dto.ItemHistoryDto, blockchain: BlockchainDto): ItemHistoryDto {
        return when (source) {
            is ItemRoyaltyDto -> EthItemConverter.convert(source, blockchain)
            is ItemTransferDto -> EthItemConverter.convert(source, blockchain)
        }
    }
}
