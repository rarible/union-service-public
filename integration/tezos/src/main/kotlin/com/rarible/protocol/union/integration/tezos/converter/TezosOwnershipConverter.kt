package com.rarible.protocol.union.integration.tezos.converter

import com.rarible.protocol.tezos.dto.NftOwnershipDto
import com.rarible.protocol.tezos.dto.NftOwnershipsDto
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipIdDto

object TezosOwnershipConverter {

    fun convert(ownership: NftOwnershipDto, blockchain: BlockchainDto): UnionOwnership {
        val tokenId = ownership.tokenId
        val owner = UnionAddressConverter.convert(blockchain, ownership.owner)

        return UnionOwnership(
            id = OwnershipIdDto(
                blockchain = blockchain,
                contract = ownership.contract,
                tokenId = tokenId,
                owner = owner
            ),
            value = ownership.value,
            createdAt = ownership.createdAt,
            creators = ownership.creators.map { TezosConverter.convertToCreator(it, blockchain) },
            lazyValue = ownership.lazyValue,
            pending = emptyList() // In Union we won't use this field for Tezos
        )
    }

    fun convert(page: NftOwnershipsDto, blockchain: BlockchainDto): Page<UnionOwnership> {
        return Page(
            total = page.total.toLong(),
            continuation = page.continuation,
            entities = page.ownerships.map { convert(it, blockchain) }
        )
    }
}

