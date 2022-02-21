package com.rarible.protocol.union.integration.solana.converter

import com.rarible.protocol.union.core.model.UnionItem
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.BlockchainGroupDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.integration.solana.data.randomSolanaBalanceDto
import com.rarible.protocol.union.integration.solana.data.randomSolanaItemId
import com.rarible.protocol.union.integration.solana.data.randomSolanaTokenDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

class SolanaItemConverterTest {

    @Test
    fun `solana ownership`() {
        val balanceDto = randomSolanaBalanceDto()

        assertThat(SolanaBalanceConverter.convert(balanceDto)).isEqualTo(
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

    @Test
    fun `solana item`() {
        val tokenDto = randomSolanaTokenDto(randomSolanaItemId())

        assertThat(SolanaItemConverter.convert(tokenDto, BlockchainDto.SOLANA)).isEqualTo(
            UnionItem(
                id = ItemIdDto(
                    blockchain = BlockchainDto.SOLANA,
                    value = tokenDto.address
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
