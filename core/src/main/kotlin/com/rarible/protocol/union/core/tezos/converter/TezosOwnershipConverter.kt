package com.rarible.protocol.union.core.tezos.converter

import com.rarible.protocol.tezos.dto.NftOwnerShipsDto
import com.rarible.protocol.tezos.dto.NftOwnershipDto
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.UnionOwnershipDto
import java.math.BigInteger

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
            createdAt = ownership.date, //todo ask tezos to add this
            contract = contract,
            tokenId = tokenId,
            owner = owner,
            lazyValue = BigInteger.ZERO
        )
    }

    //todo tell tezos about NftOwnerShips
    fun convert(page: NftOwnerShipsDto, blockchain: BlockchainDto): Page<UnionOwnershipDto> {
        return Page(
            total = page.total.toLong(),
            continuation = page.continuation,
            entities = page.ownerships.map { convert(it, blockchain) }
        )
    }
}

