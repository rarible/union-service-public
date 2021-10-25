package com.rarible.protocol.union.integration.flow.converter

import com.rarible.protocol.dto.FlowNftOwnershipDto
import com.rarible.protocol.dto.FlowNftOwnershipsDto
import com.rarible.protocol.union.core.continuation.page.Page
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import java.math.BigInteger

object FlowOwnershipConverter {

    fun convert(ownership: FlowNftOwnershipDto, blockchain: BlockchainDto): UnionOwnership {
        val contract = UnionAddressConverter.convert(ownership.contract, blockchain)
        val tokenId = ownership.tokenId
        val owner = UnionAddressConverter.convert(ownership.owner, blockchain)

        return UnionOwnership(
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

    fun convert(page: FlowNftOwnershipsDto, blockchain: BlockchainDto): Page<UnionOwnership> {
        return Page(
            total = page.total,
            continuation = page.continuation,
            entities = page.ownerships.map { convert(it, blockchain) }
        )
    }
}

