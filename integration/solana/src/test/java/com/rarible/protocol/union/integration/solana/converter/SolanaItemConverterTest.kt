package com.rarible.protocol.union.integration.solana.converter

import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CreatorDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.integration.solana.data.randomSolanaItemId
import com.rarible.protocol.union.integration.solana.data.randomSolanaTokenDto
import com.rarible.solana.protocol.dto.JsonCollectionDto
import com.rarible.solana.protocol.dto.OnChainCollectionDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

class SolanaItemConverterTest {
    @Test
    fun `solana item`() {
        val tokenDto = randomSolanaTokenDto(randomSolanaItemId())

        assertThat(SolanaItemConverter.convert(tokenDto, BlockchainDto.SOLANA)).isEqualTo(
            UnionItem(
                id = ItemIdDto(
                    blockchain = BlockchainDto.SOLANA,
                    value = tokenDto.address
                ),
                collection = CollectionIdDto(
                    blockchain = BlockchainDto.SOLANA,
                    value = when (val collection = tokenDto.collection!!) {
                        is JsonCollectionDto -> collection.hash
                        is OnChainCollectionDto -> collection.address
                    }
                ),
                creators = tokenDto.creators.orEmpty().map {
                    CreatorDto(
                        account = UnionAddressConverter.convert(
                            blockchain = BlockchainDto.SOLANA,
                            source = it.address
                        ),
                        value = it.share
                    )
                },
                owners = emptyList(),
                royalties = emptyList(),
                lazySupply = BigInteger.ZERO,
                pending = emptyList(),
                mintedAt = tokenDto.createdAt,
                lastUpdatedAt = tokenDto.updatedAt,
                supply = tokenDto.supply,
                meta = null,
                deleted = false
            )
        )
    }
}
