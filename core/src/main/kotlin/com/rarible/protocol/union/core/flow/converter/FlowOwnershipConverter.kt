package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.FlowNftOwnershipDto
import com.rarible.protocol.dto.FlowNftOwnershipsDto
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.OwnershipsDto
import java.math.BigInteger

object FlowOwnershipConverter {

    fun convert(ownership: FlowNftOwnershipDto, blockchain: BlockchainDto): OwnershipDto {
        val contract = FlowContractConverter.convert(ownership.contract!!, blockchain) // TODO should be not null?
        val tokenId = ownership.tokenId.toBigInteger() // TODO should be BigInt
        val owner = UnionAddressConverter.convert(ownership.owner, blockchain)

        return OwnershipDto(
            id = OwnershipIdDto(
                blockchain = blockchain,
                token = contract,
                tokenId = tokenId,
                owner = owner
            ),
            value = BigInteger.ONE,//TODO: Is it right?
            createdAt = ownership.createdAt,
            contract = contract,
            tokenId = tokenId,
            owner = owner,
            lazyValue = BigInteger.ZERO
            // TODO creators =
        )
    }

    fun convert(page: FlowNftOwnershipsDto, blockchain: BlockchainDto): OwnershipsDto {
        return OwnershipsDto(
            total = page.total.toLong(), // TODO should be long
            continuation = page.continuation,
            ownerships = page.ownerships.map { convert(it, blockchain) }
        )
    }
}

