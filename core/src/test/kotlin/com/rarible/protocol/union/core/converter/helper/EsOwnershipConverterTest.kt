package com.rarible.protocol.union.core.converter.helper

import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.converter.EsOwnershipConverter
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipDto
import com.rarible.protocol.union.dto.ext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import randomAssetTypeErc20Dto
import randomOrderDto
import randomOwnership
import randomOwnershipId
import java.util.stream.Stream

internal class EsOwnershipConverterTest {

    private val converter = EsOwnershipConverter

    @ParameterizedTest
    @MethodSource("source")
    fun `should convert`(source: OwnershipDto) {
        val result = converter.convert(source)
        assertThat(result.ownershipId).isEqualTo(source.id.fullId())
        assertThat(result.originalOwnershipId).isNull()
        assertThat(result.id).isEqualTo(source.id.fullId())
        assertThat(result.blockchain).isEqualTo(source.blockchain)
        assertThat(result.itemId).isEqualTo(source.itemId?.fullId())
        assertThat(result.collection).isEqualTo(source.collection?.fullId())
        assertThat(result.owner).isEqualTo(source.owner.fullId())
        assertThat(result.date).isEqualTo(source.createdAt)
        assertThat(result.bestSellAmount).isEqualTo(source.bestSellOrder?.take?.value?.toDouble())
        assertThat(result.bestSellCurrency).isEqualTo(source.blockchain.name + ":" + source.bestSellOrder?.take?.type?.ext?.currencyAddress())
        assertThat(result.bestSellMarketplace).isEqualTo(source.bestSellOrder?.platform?.name)
    }

    @Test
    fun `should convert with long original id`() {
        // given
        val source = randomOwnership(id = randomOwnershipId(BlockchainDto.TEZOS, randomString(513)))

        // when
        val actual = converter.convert(source)

        // then
        assertThat(actual.ownershipId).hasSize(64)
        assertThat(actual.originalOwnershipId).isEqualTo(source.id.fullId())
        assertThat(actual.id).isEqualTo(source.id.fullId())
    }

    companion object {
        @JvmStatic
        fun source(): Stream<Arguments> = BlockchainDto.values().map {
            randomOwnership(
                id = randomOwnershipId(it),
                bestSellOrder = randomOrderDto(take = AssetDto(randomAssetTypeErc20Dto(it), randomBigDecimal()))
            )
        }.map { Arguments.of(it) }.stream()
    }
}
