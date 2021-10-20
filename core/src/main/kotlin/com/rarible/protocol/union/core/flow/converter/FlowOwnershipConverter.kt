package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.FlowNftOwnershipDto
import com.rarible.protocol.dto.FlowNftOwnershipsDto
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionOwnershipDto
import java.math.BigInteger

object FlowOwnershipConverter {

    fun convert(ownership: FlowNftOwnershipDto, blockchain: BlockchainDto): UnionOwnershipDto {
        val contract = UnionAddressConverter.convert(ownership.contract!!, blockchain) // TODO FLOW should be not null?
        val tokenId = ownership.tokenId.toBigInteger() // TODO FLOW should be BigInt
        val owner = UnionAddressConverter.convert(ownership.owner, blockchain)

        return UnionOwnershipDto(
            id = OwnershipIdDto(
                blockchain = blockchain,
                token = contract,
                tokenId = tokenId,
                owner = owner
            ),
            value = BigInteger.ONE, // TODO FLOW always one?
            createdAt = ownership.createdAt,
            contract = contract,
            tokenId = tokenId,
            owner = owner,
            lazyValue = BigInteger.ZERO,
            creators = ownership.creators.map { FlowConverter.convertToCreator(it, blockchain) }
        )
    }

    fun convert(page: FlowNftOwnershipsDto, blockchain: BlockchainDto): Page<UnionOwnershipDto> {
        return Page(
            total = page.total,
            continuation = page.continuation,
            entities = page.ownerships.map { convert(it, blockchain) }
        )
    }
}

