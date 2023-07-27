package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomLong
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.data.randomImxAsset
import com.rarible.protocol.union.integration.data.randomImxCollectionShort
import com.rarible.protocol.union.integration.data.randomImxMint
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexAsset
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexFee
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.math.BigInteger

class ImxItemConverterTest {

    private val blockchain = BlockchainDto.IMMUTABLEX

    @Test
    fun `convert asset`() {
        val imxItem = randomImxAsset()
        val creator = randomAddress().prefixed()

        val item = ImxItemConverter.convert(imxItem, creator, blockchain)

        assertThat(item.id.value).isEqualTo(imxItem.encodedItemId())
        assertThat(item.collection!!.value).isEqualTo(imxItem.tokenAddress)

        assertThat(item.creators).hasSize(1)
        assertThat(item.creators[0].value).isEqualTo(10000)
        assertThat(item.creators[0].account.value).isEqualTo(creator)

        assertThat(item.lazySupply).isEqualTo(BigInteger.ZERO)
        assertThat(item.deleted).isEqualTo(false)

        assertThat(item.supply).isEqualTo(BigInteger.ONE)
        assertThat(item.mintedAt).isEqualTo(imxItem.createdAt)
        assertThat(item.lastUpdatedAt).isEqualTo(imxItem.updatedAt)
    }

    @Test
    fun `convert asset - non-required fields are missing`() {
        val imxItem = ImmutablexAsset(
            collection = randomImxCollectionShort(),
            tokenAddress = randomAddress().prefixed(),
            tokenId = randomLong().toString(),

            createdAt = null,
            description = null,
            fees = emptyList(),
            id = null,
            imageUrl = null,
            metadata = emptyMap(),
            name = null,
            status = null,
            uri = null,
            updatedAt = null,
            user = null
        )

        val item = ImxItemConverter.convert(imxItem, null, blockchain)

        assertThat(item.id.value).isEqualTo(imxItem.encodedItemId())
        assertThat(item.deleted).isEqualTo(true)
        assertThat(item.supply).isEqualTo(BigInteger.ZERO)
    }

    @Test
    fun `convert mint`() {
        val mint = randomImxMint()
        val creator = randomAddress().prefixed()

        val item = ImxItemConverter.convert(mint, creator, blockchain)

        assertThat(item.id.value).isEqualTo(mint.encodedItemId())
        assertThat(item.collection!!.value).isEqualTo(mint.token.data.tokenAddress)

        assertThat(item.creators).hasSize(1)
        assertThat(item.creators[0].value).isEqualTo(10000)
        assertThat(item.creators[0].account.value).isEqualTo(creator)

        assertThat(item.lazySupply).isEqualTo(BigInteger.ZERO)
        assertThat(item.deleted).isEqualTo(false)

        assertThat(item.supply).isEqualTo(BigInteger.ONE)
        assertThat(item.mintedAt).isEqualTo(mint.timestamp)
        assertThat(item.lastUpdatedAt).isEqualTo(mint.timestamp)
    }

    @Test
    fun `convert asset - without creator`() {
        val item = ImxItemConverter.convert(randomImxAsset(), null, blockchain)

        assertThat(item.creators).hasSize(0)
    }

    @Test
    fun `convert mint - without creator`() {
        val item = ImxItemConverter.convert(randomImxMint(), null, blockchain)

        assertThat(item.creators).hasSize(0)
    }

    @ParameterizedTest
    @ValueSource(strings = ["eth", "preparing_withdrawal", "withdrawable", "burned"])
    fun `convert asset - deleted`(status: String) {
        val imxItem = randomImxAsset().copy(status = status)

        val item = ImxItemConverter.convert(imxItem, null, blockchain)

        assertThat(item.deleted).isEqualTo(true)
        assertThat(item.supply).isEqualTo(BigInteger.ZERO)
    }

    @Test
    fun `convert asset - missing createdAt`() {
        val imxItem = randomImxAsset().copy(createdAt = null)

        val item = ImxItemConverter.convert(imxItem, null, blockchain)

        // Nothing to do, we can just use updatedAt here
        assertThat(item.mintedAt).isEqualTo(imxItem.updatedAt)
        assertThat(item.lastUpdatedAt).isEqualTo(imxItem.updatedAt)
    }

    @Test
    fun `convert royalty`() {
        val royalty = ImmutablexFee(randomAddress().prefixed(), BigDecimal("5"), "royalty")
        val fee = ImmutablexFee(randomAddress().prefixed(), BigDecimal("10"), "protocol")
        val asset = randomImxAsset().copy(fees = listOf(royalty, fee))

        val fees = ImxItemConverter.convertToRoyaltyDto(asset, blockchain)

        assertThat(fees).hasSize(1)
        assertThat(fees[0].value).isEqualTo(500)
        assertThat(fees[0].account.value).isEqualTo(royalty.address)
    }
}
