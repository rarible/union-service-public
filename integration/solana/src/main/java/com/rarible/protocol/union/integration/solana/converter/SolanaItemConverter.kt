package com.rarible.protocol.union.integration.solana.converter

import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.solana.protocol.dto.JsonCollectionDto
import com.rarible.solana.protocol.dto.OnChainCollectionDto
import com.rarible.solana.protocol.dto.TokenCreatorPartDto
import com.rarible.solana.protocol.dto.TokenDto
import java.math.BigInteger

object SolanaItemConverter {

    fun convert(token: TokenDto, blockchain: BlockchainDto): UnionItem {
        return UnionItem(
            id = ItemIdDto(
                blockchain = blockchain,
                value = token.address
            ),
            creators = token.creators.orEmpty().map { convert(it, blockchain) },
            collection = when (val collection = token.collection) {
                is JsonCollectionDto -> CollectionIdDto(blockchain, collection.hash)
                is OnChainCollectionDto -> CollectionIdDto(blockchain, collection.address)
                null -> null
            },
            owners = emptyList(),
            royalties = emptyList(), // TODO SOLANA: royalties are not supported yet.
            lazySupply = BigInteger.ZERO,
            pending = emptyList(),
            mintedAt = token.createdAt,
            lastUpdatedAt = token.updatedAt,
            supply = token.supply,
            meta = null,
            deleted = token.closed
        )
    }

    private fun convert(creator: TokenCreatorPartDto, blockchain: BlockchainDto) =
        CreatorDto(
            account = UnionAddressConverter.convert(blockchain, creator.address),
            value = creator.share
        )
}
