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
        val tokenId = ownership.tokenId
        val owner = UnionAddressConverter.convert(blockchain, ownership.owner)

        return UnionOwnership(
            id = OwnershipIdDto(
                blockchain = blockchain,
                contract = ownership.contract,
                tokenId = tokenId,
                owner = owner
            ),
            value = BigInteger.ONE, // TODO FLOW always one?
            createdAt = ownership.createdAt,
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

