package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.protocol.dto.ItemHistoryDto
import com.rarible.protocol.dto.ItemRoyaltyDto
import com.rarible.protocol.dto.ItemTransferDto
import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.dto.NftOwnershipsDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UnionItemHistoryDto
import com.rarible.protocol.union.dto.UnionOwnershipDto
import com.rarible.protocol.union.dto.UnionOwnershipIdDto
import com.rarible.protocol.union.dto.UnionOwnershipsDto

object EthUnionOwnershipConverter {

    fun convert(source: NftOwnershipDto, blockchain: BlockchainDto): UnionOwnershipDto {
        return UnionOwnershipDto(
            id = UnionOwnershipIdDto(
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

    fun convert(page: NftOwnershipsDto, blockchain: BlockchainDto): UnionOwnershipsDto {
        return UnionOwnershipsDto(
            total = page.total,
            continuation = page.continuation,
            ownerships = page.ownerships.map { convert(it, blockchain) }
        )
    }

    private fun convert(source: ItemHistoryDto, blockchain: BlockchainDto): UnionItemHistoryDto {
        return when (source) {
            is ItemRoyaltyDto -> EthUnionItemConverter.convert(source, blockchain)
            is ItemTransferDto -> EthUnionItemConverter.convert(source, blockchain)
        }
    }
}
