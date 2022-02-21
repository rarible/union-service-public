package com.rarible.protocol.union.integration.solana.converter

import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.solana.protocol.dto.BalanceDto
import java.math.BigInteger

object SolanaOwnershipConverter {
    // TODO think about collection & creators
    fun convert(balance: BalanceDto): UnionOwnership {
        return UnionOwnership(
            OwnershipIdDto(
                blockchain = BlockchainDto.SOLANA,
                itemIdValue = balance.mint,
                owner = UnionAddress(
                    blockchainGroup = BlockchainGroupDto.SOLANA,
                    value = balance.owner
                )
            ),
            collection = null,
            value = balance.value,
            createdAt = balance.createdAt,
            creators = emptyList(),
            lazyValue = BigInteger.ZERO,
            pending = emptyList()
        )
    }
}
