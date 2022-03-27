package com.rarible.protocol.union.integration.solana.converter

import com.rarible.protocol.solana.dto.BalanceDto
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.group
import java.math.BigInteger

object SolanaOwnershipConverter {

    // TODO SOLANA think about collection & creators
    fun convert(balance: BalanceDto, blockchain: BlockchainDto): UnionOwnership {
        return UnionOwnership(
            OwnershipIdDto(
                blockchain = blockchain,
                itemIdValue = balance.mint,
                owner = UnionAddress(
                    blockchainGroup = blockchain.group(),
                    value = balance.owner
                )
            ),
            // TODO it MUST be not-null
            collection = balance.collection?.let { CollectionIdDto(blockchain, it) },
            value = balance.value,
            createdAt = balance.createdAt,
            creators = emptyList(),
            lazyValue = BigInteger.ZERO,
            pending = emptyList()
        )
    }
}
