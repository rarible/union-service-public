package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.FlowNftOwnershipDto
import com.rarible.protocol.dto.FlowNftOwnershipsDto
import com.rarible.protocol.union.dto.FlowBlockchainDto
import com.rarible.protocol.union.dto.FlowOwnershipDto
import com.rarible.protocol.union.dto.UnionOwnershipsDto
import com.rarible.protocol.union.dto.flow.FlowOwnershipIdDto
import java.math.BigInteger

object FlowUnionOwnershipConverter {

    fun convert(ownership: FlowNftOwnershipDto, blockchain: FlowBlockchainDto): FlowOwnershipDto {
        val contract = FlowContractConverter.convert(ownership.contract!!, blockchain) // TODO should be not null?
        val tokenId = ownership.tokenId.toBigInteger() // TODO should be BigInt
        val owner = FlowAddressConverter.convert(ownership.owner, blockchain)

        return FlowOwnershipDto(
            id = FlowOwnershipIdDto(
                blockchain = blockchain,
                token = contract,
                tokenId = tokenId,
                owner = owner
            ),
            value = BigInteger.ONE,//TODO: Is it right?
            createdAt = ownership.createdAt,
            contract = contract,
            tokenId = tokenId,
            owners = listOf(owner)
            // TODO creators =
        )
    }

    fun convert(page: FlowNftOwnershipsDto, blockchain: FlowBlockchainDto): UnionOwnershipsDto {
        return UnionOwnershipsDto(
            total = page.total.toLong(), // TODO should be long
            continuation = page.continuation,
            ownerships = page.ownerships.map { convert(it, blockchain) }
        )
    }
}

