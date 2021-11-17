package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.protocol.dto.ActivityFilterAllDto
import com.rarible.protocol.dto.ActivityFilterAllTypeDto
import com.rarible.protocol.dto.ActivityFilterByCollectionDto
import com.rarible.protocol.dto.ActivityFilterByCollectionTypeDto
import com.rarible.protocol.dto.ActivityFilterByItemDto
import com.rarible.protocol.dto.ActivityFilterByItemTypeDto
import com.rarible.protocol.dto.ActivityFilterByUserDto
import com.rarible.protocol.dto.ActivityFilterByUserTypeDto
import com.rarible.protocol.dto.NftActivityFilterAllDto
import com.rarible.protocol.dto.NftActivityFilterByCollectionDto
import com.rarible.protocol.dto.NftActivityFilterByItemDto
import com.rarible.protocol.dto.NftActivityFilterByUserDto
import com.rarible.protocol.dto.OrderActivityFilterAllDto
import com.rarible.protocol.dto.OrderActivityFilterByCollectionDto
import com.rarible.protocol.dto.OrderActivityFilterByItemDto
import com.rarible.protocol.dto.OrderActivityFilterByUserDto
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class EthActivityFilterConverterTest {

    @Test
    fun `eth activities all`() {
        val filter = ActivityFilterAllDto(
            types = ActivityFilterAllTypeDto.values().asList()
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
            types = ActivityFilterByCollectionTypeDto.values().asList(),
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
            types = ActivityFilterByItemTypeDto.values().asList(),
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
        val now = Instant.now()
        val oneWeekAgo = now.minus(7, ChronoUnit.DAYS).epochSecond
        val today = now.epochSecond
        val filter = ActivityFilterByUserDto(
            types = ActivityFilterByUserTypeDto.values().asList(),
            users = listOf(randomAddress(), randomAddress()),
            from = oneWeekAgo,
            to = today
        )

        val itemFilter = EthActivityFilterConverter.asItemActivityFilter(filter) as NftActivityFilterByUserDto
        val orderFilter = EthActivityFilterConverter.asOrderActivityFilter(filter) as OrderActivityFilterByUserDto

        assertThat(itemFilter.users).isEqualTo(filter.users)
        assertThat(itemFilter.types).isEqualTo(NftActivityFilterByUserDto.Types.values().toList())
        assertThat(itemFilter.from).isEqualTo(filter.from)
        assertThat(itemFilter.to).isEqualTo(filter.to)

        assertThat(orderFilter.users).isEqualTo(filter.users)
        assertThat(orderFilter.types).isEqualTo(OrderActivityFilterByUserDto.Types.values().toList())
        assertThat(orderFilter.from).isEqualTo(filter.from)
        assertThat(orderFilter.to).isEqualTo(filter.to)
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
