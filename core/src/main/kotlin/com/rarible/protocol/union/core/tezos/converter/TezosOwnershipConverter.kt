package com.rarible.protocol.union.core.tezos.converter

import com.rarible.protocol.tezos.dto.NftOwnershipDto
import com.rarible.protocol.tezos.dto.NftOwnershipsDto
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.UnionOwnershipDto

object TezosOwnershipConverter {

    fun convert(ownership: NftOwnershipDto, blockchain: BlockchainDto): UnionOwnershipDto {
        val contract = UnionAddress(blockchain, ownership.contract)
        val tokenId = ownership.tokenId
        val owner = UnionAddressConverter.convert(ownership.owner, blockchain)

        return UnionOwnershipDto(
            id = OwnershipIdDto(
                blockchain = blockchain,
                token = contract,
                tokenId = tokenId,
                owner = owner
            ),
            value = ownership.value,
            createdAt = ownership.date, //TODO ask TEZOS to add this
            contract = contract,
            tokenId = tokenId,
            owner = owner,
            creators = ownership.creators.map { TezosConverter.convertToCreator(it, blockchain) },
            //lazyValue = BigInteger.ZERO,
            lazyValue = ownership.lazyValue,
            pending = emptyList() // TODO won't populate for now
        )
    }

    fun convert(page: NftOwnershipsDto, blockchain: BlockchainDto): Page<UnionOwnershipDto> {
        return Page(
            total = page.total.toLong(),
            continuation = page.continuation,
            entities = page.ownerships.map { convert(it, blockchain) }
        )
    }
}

