package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.protocol.dto.ItemRoyaltyDto
import com.rarible.protocol.dto.ItemTransferDto
import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.dto.NftOwnershipsDto
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemHistoryDto
import com.rarible.protocol.union.dto.OwnershipIdDto

object EthOwnershipConverter {

    fun convert(source: NftOwnershipDto, blockchain: BlockchainDto): UnionOwnership {
        return UnionOwnership(
            id = OwnershipIdDto(
                token = EthConverter.convert(source.contract, blockchain),
                tokenId = source.tokenId,
                owner = EthConverter.convert(source.owner, blockchain),
                blockchain = blockchain
            ),
            value = source.value,
            createdAt = source.date,
            contract = EthConverter.convert(source.contract, blockchain),
            tokenId = source.tokenId,
            owner = EthConverter.convert(source.owner, blockchain),
            creators = source.creators.map { EthConverter.convertToCreator(it, blockchain) },
            lazyValue = source.lazyValue,
            pending = source.pending.map { convert(it, blockchain) }
        )
    }

    fun convert(page: NftOwnershipsDto, blockchain: BlockchainDto): Page<UnionOwnership> {
        return Page(
            total = page.total,
            continuation = page.continuation,
            entities = page.ownerships.map { convert(it, blockchain) }
        )
    }

    private fun convert(source: com.rarible.protocol.dto.ItemHistoryDto, blockchain: BlockchainDto): ItemHistoryDto {
        return when (source) {
            is ItemRoyaltyDto -> EthItemConverter.convert(source, blockchain)
            is ItemTransferDto -> EthItemConverter.convert(source, blockchain)
        }
    }
}
