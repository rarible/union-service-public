package com.rarible.protocol.union.core.ethereum.converter

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.protocol.dto.*
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EthActivityFilterConverterTest {

    @Test
    fun `eth activities all`() {
        val filter = ActivityFilterAllDto(
            types = ActivityFilterAllDto.Types.values().asList()
        )

        val itemFilter = EthActivityFilterConverter.asItemActivityFilter(filter) as NftActivityFilterAllDto
        val orderFilter = EthActivityFilterConverter.asOrderActivityFilter(filter) as OrderActivityFilterAllDto

        assertThat(itemFilter.types).isEqualTo(NftActivityFilterAllDto.Types.values().toList())
        assertThat(orderFilter.types).isEqualTo(OrderActivityFilterAllDto.Types.values().toList())
    }

    @Test
    fun `eth activities all - empty`() {
        val filter = ActivityFilterAllDto(
            types = listOf()
        )

        val itemFilter = EthActivityFilterConverter.asItemActivityFilter(filter)
        val orderFilter = EthActivityFilterConverter.asOrderActivityFilter(filter)

        assertThat(itemFilter).isNull()
        assertThat(orderFilter).isNull()
    }

    @Test
    fun `eth activities by collection`() {
        val filter = ActivityFilterByCollectionDto(
            types = ActivityFilterByCollectionDto.Types.values().asList(),
            contract = randomAddress()
        )

        val itemFilter = EthActivityFilterConverter.asItemActivityFilter(filter) as NftActivityFilterByCollectionDto
        val orderFilter = EthActivityFilterConverter.asOrderActivityFilter(filter) as OrderActivityFilterByCollectionDto

        assertThat(itemFilter.contract).isEqualTo(filter.contract)
        assertThat(itemFilter.types).isEqualTo(NftActivityFilterByCollectionDto.Types.values().toList())

        assertThat(orderFilter.contract).isEqualTo(filter.contract)
        assertThat(orderFilter.types).isEqualTo(OrderActivityFilterByCollectionDto.Types.values().toList())
    }

    @Test
    fun `eth activities by collection - empty`() {
        val filter = ActivityFilterByCollectionDto(
            types = listOf(),
            contract = randomAddress()
        )

        val itemFilter = EthActivityFilterConverter.asItemActivityFilter(filter)
        val orderFilter = EthActivityFilterConverter.asOrderActivityFilter(filter)

        assertThat(itemFilter).isNull()
        assertThat(orderFilter).isNull()
    }

    @Test
    fun `eth activities by item`() {
        val filter = ActivityFilterByItemDto(
            types = ActivityFilterByItemDto.Types.values().asList(),
            contract = randomAddress(),
            tokenId = randomBigInt()
        )

        val itemFilter = EthActivityFilterConverter.asItemActivityFilter(filter) as NftActivityFilterByItemDto
        val orderFilter = EthActivityFilterConverter.asOrderActivityFilter(filter) as OrderActivityFilterByItemDto

        assertThat(itemFilter.contract).isEqualTo(filter.contract)
        assertThat(itemFilter.tokenId).isEqualTo(filter.tokenId)
        assertThat(itemFilter.types).isEqualTo(NftActivityFilterByItemDto.Types.values().toList())

        assertThat(orderFilter.contract).isEqualTo(filter.contract)
        assertThat(orderFilter.tokenId).isEqualTo(filter.tokenId)
        assertThat(orderFilter.types).isEqualTo(OrderActivityFilterByItemDto.Types.values().toList())
    }

    @Test
    fun `eth activities by item - empty`() {
        val filter = ActivityFilterByItemDto(
            types = listOf(),
            contract = randomAddress(),
            tokenId = randomBigInt()
        )

        val itemFilter = EthActivityFilterConverter.asItemActivityFilter(filter)
        val orderFilter = EthActivityFilterConverter.asOrderActivityFilter(filter)

        assertThat(itemFilter).isNull()
        assertThat(orderFilter).isNull()
    }

    @Test
    fun `eth activities by user`() {
        val filter = ActivityFilterByUserDto(
            types = ActivityFilterByUserDto.Types.values().asList(),
            users = listOf(randomAddress(), randomAddress())
        )

        val itemFilter = EthActivityFilterConverter.asItemActivityFilter(filter) as NftActivityFilterByUserDto
        val orderFilter = EthActivityFilterConverter.asOrderActivityFilter(filter) as OrderActivityFilterByUserDto

        assertThat(itemFilter.users).isEqualTo(filter.users)
        assertThat(itemFilter.types).isEqualTo(NftActivityFilterByUserDto.Types.values().toList())

        assertThat(orderFilter.users).isEqualTo(filter.users)
        assertThat(orderFilter.types).isEqualTo(OrderActivityFilterByUserDto.Types.values().toList())
    }

    @Test
    fun `eth activities by user - empty`() {
        val filter = ActivityFilterByUserDto(
            types = listOf(),
            users = listOf(randomAddress(), randomAddress())
        )

        val itemFilter = EthActivityFilterConverter.asItemActivityFilter(filter)
        val orderFilter = EthActivityFilterConverter.asOrderActivityFilter(filter)

        assertThat(itemFilter).isNull()
        assertThat(orderFilter).isNull()
    }

}