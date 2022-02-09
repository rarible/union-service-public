package com.rarible.protocol.union.integration.solana.converter

import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.solana.protocol.dto.TokenCreatorPartDto
import com.rarible.solana.protocol.dto.TokenDto
import java.math.BigInteger

object SolanaItemConverter {
    fun convert(token: TokenDto, blockchainDto: BlockchainDto): UnionItem {
        return UnionItem(
            id = ItemIdDto(
                blockchain = BlockchainDto.SOLANA,
                contract = token.address,
                tokenId = BigInteger.ZERO // TODO[solana]: not applicable.
            ),
            creators = emptyList(), // TODO[solana]: set up creators.
            owners = emptyList(),
            royalties = emptyList(), // TODO[solana]: royalties are not supported yet.
            lazySupply = BigInteger.ZERO,
            pending = emptyList(),
            mintedAt = token.createdAt,
            lastUpdatedAt = token.updatedAt,
            supply = token.supply,
            meta = null,
            deleted = token.closed
        )
    }

    private fun TokenCreatorPartDto.convert(blockchainDto: BlockchainDto) =
        CreatorDto(
            account = UnionAddressConverter.convert(blockchainDto, address),
            value = share
        )
}
