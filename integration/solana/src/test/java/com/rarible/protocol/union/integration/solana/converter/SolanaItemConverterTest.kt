package com.rarible.protocol.union.integration.solana.converter

import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.integration.solana.data.randomSolanaItemId
import com.rarible.protocol.union.integration.solana.data.randomSolanaTokenDto
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
                    contract = tokenDto.address,
                    tokenId = BigInteger.ZERO // TODO[solana]: not applicable.
                ),
                collection = null,
                creators = emptyList(),
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
