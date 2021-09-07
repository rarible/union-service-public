package com.rarible.protocol.union.core.flow.converter

import com.rarible.protocol.dto.FlowNftOwnershipDto
import com.rarible.protocol.union.dto.FlowBlockchainDto
import com.rarible.protocol.union.dto.FlowOwnershipDto
import com.rarible.protocol.union.dto.flow.FlowOwnershipIdProvider
import java.math.BigInteger

object FlowUnionOwnershipConverter {

    fun convert(ownership: FlowNftOwnershipDto, blockchain: FlowBlockchainDto): FlowOwnershipDto {
        val contract = FlowContractConverter.convert(ownership.contract!!, blockchain) // TODO should be not null?
        val tokenId = ownership.tokenId.toBigInteger() // TODO should be BigInt
        val owner = FlowAddressConverter.convert(ownership.owner, blockchain)

        // TODO add blockchain
        return FlowOwnershipDto(
            id = FlowOwnershipIdProvider.create(contract, tokenId, owner, blockchain),
            value = BigInteger.ONE,//TODO: Is it right?
            createdAt = ownership.createdAt,
            contract = contract,
            tokenId = tokenId,
            owner = listOf(owner)
        )
    }
}

