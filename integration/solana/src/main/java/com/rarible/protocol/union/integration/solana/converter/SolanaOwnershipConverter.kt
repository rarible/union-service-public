package com.rarible.protocol.union.integration.solana.converter

import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.group
import com.rarible.solana.protocol.dto.BalanceDto
import com.rarible.solana.protocol.dto.JsonCollectionDto
import com.rarible.solana.protocol.dto.OnChainCollectionDto
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
            collection = when (val collection = balance.collection) {
                is JsonCollectionDto -> CollectionIdDto(blockchain, collection.hash)
                is OnChainCollectionDto -> CollectionIdDto(blockchain, collection.address)
                null -> null
            },
            value = balance.value,
            createdAt = balance.createdAt,
            creators = emptyList(),
            lazyValue = BigInteger.ZERO,
            pending = emptyList()
        )
    }
}
