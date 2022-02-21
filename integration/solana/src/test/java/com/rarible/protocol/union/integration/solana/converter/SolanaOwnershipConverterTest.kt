package com.rarible.protocol.union.integration.solana.converter

import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.integration.solana.data.randomSolanaBalanceDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

class SolanaOwnershipConverterTest {
    @Test
    fun `solana ownership`() {
        val balanceDto = randomSolanaBalanceDto()

        assertThat(SolanaOwnershipConverter.convert(balanceDto)).isEqualTo(
            UnionOwnership(
                OwnershipIdDto(
                    blockchain = BlockchainDto.SOLANA,
                    itemIdValue = balanceDto.mint,
                    owner = UnionAddress(
                        blockchainGroup = BlockchainGroupDto.SOLANA,
                        value = balanceDto.owner
                    )
                ),
                collection = null,
                value = balanceDto.value,
                createdAt = balanceDto.createdAt,
                creators = emptyList(),
                lazyValue = BigInteger.ZERO,
                pending = emptyList()
            )
        )
    }
}
