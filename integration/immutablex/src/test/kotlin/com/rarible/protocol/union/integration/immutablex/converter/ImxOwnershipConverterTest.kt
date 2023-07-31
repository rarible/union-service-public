package com.rarible.protocol.union.integration.immutablex.converter

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomLong
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.data.randomImxAsset
import com.rarible.protocol.union.integration.data.randomImxCollectionShort
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexAsset
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigInteger

class ImxOwnershipConverterTest {

    private val blockchain = BlockchainDto.IMMUTABLEX

    @Test
    fun `convert ownership`() {
        val imxItem = randomImxAsset()
        val creator = randomAddress().prefixed()

        val ownership = ImxOwnershipConverter.convert(imxItem, creator, blockchain)

        assertThat(ownership.id.itemIdValue).isEqualTo(imxItem.encodedItemId())
        assertThat(ownership.id.owner.value).isEqualTo(imxItem.user)

        assertThat(ownership.creators).hasSize(1)
        assertThat(ownership.creators[0].value).isEqualTo(10000)
        assertThat(ownership.creators[0].account.value).isEqualTo(creator)

        assertThat(ownership.value).isEqualTo(BigInteger.ONE)
        assertThat(ownership.createdAt).isEqualTo(imxItem.updatedAt)
        assertThat(ownership.lastUpdatedAt).isEqualTo(imxItem.updatedAt)

        assertThat(ownership.lazyValue).isEqualTo(BigInteger.ZERO)
        assertThat(ownership.collection!!.value).isEqualTo(imxItem.tokenAddress)
    }

    @Test
    fun `convert ownership - non-required fields are missing`() {
        val imxItem = ImmutablexAsset(
            collection = randomImxCollectionShort(),
            tokenAddress = randomAddress().prefixed(),
            tokenId = randomLong().toString(),
            user = randomAddress().prefixed(),

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
        )

        val ownership = ImxOwnershipConverter.convert(imxItem, null, blockchain)

        assertThat(ownership.id.itemIdValue).isEqualTo(imxItem.encodedItemId())
        assertThat(ownership.id.owner.value).isEqualTo(imxItem.user)

        assertThat(ownership.value).isEqualTo(BigInteger.ONE)

        assertThat(ownership.lazyValue).isEqualTo(BigInteger.ZERO)
        assertThat(ownership.collection!!.value).isEqualTo(imxItem.tokenAddress)
    }
}
