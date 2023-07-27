package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.protocol.dto.NftActivityFilterAllDto
import com.rarible.protocol.dto.NftActivityFilterByCollectionDto
import com.rarible.protocol.dto.NftActivityFilterByItemDto
import com.rarible.protocol.dto.NftActivityFilterByUserDto
import com.rarible.protocol.dto.OrderActivityFilterAllDto
import com.rarible.protocol.dto.OrderActivityFilterByCollectionDto
import com.rarible.protocol.dto.OrderActivityFilterByItemDto
import com.rarible.protocol.dto.OrderActivityFilterByUserDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class EthActivityFilterConverterTest {

    private val ethActivityConverter = EthActivityConverter(mockk())

    @Test
    fun `eth nft activities item by all type`() {
        val result = ethActivityConverter.convertToNftAllTypes(
            // To check deduplication
            ActivityTypeDto.values().toList() + ActivityTypeDto.values().toList()
        )

        assertThat(result).hasSize(3)
        assertThat(result).contains(
            NftActivityFilterAllDto.Types.TRANSFER,
            NftActivityFilterAllDto.Types.MINT,
            NftActivityFilterAllDto.Types.BURN
        )
    }

    @Test
    fun `eth nft activities item alls type - empty`() {
        val result = ethActivityConverter.convertToNftAllTypes(emptyList())
        assertThat(result).isNull()
    }

    @Test
    fun `eth activities items by collection type`() {
        val result = ethActivityConverter.convertToNftCollectionTypes(
            // To check deduplication
            ActivityTypeDto.values().toList() + ActivityTypeDto.values().toList()
        )

        assertThat(result).hasSize(3)
        assertThat(result).contains(
            NftActivityFilterByCollectionDto.Types.TRANSFER,
            NftActivityFilterByCollectionDto.Types.MINT,
            NftActivityFilterByCollectionDto.Types.BURN
        )
    }

    @Test
    fun `eth nft activities item by item type`() {
        val result = ethActivityConverter.convertToNftItemTypes(
            // To check deduplication
            ActivityTypeDto.values().toList() + ActivityTypeDto.values().toList()
        )

        assertThat(result).hasSize(3)
        assertThat(result).contains(
            NftActivityFilterByItemDto.Types.TRANSFER,
            NftActivityFilterByItemDto.Types.MINT,
            NftActivityFilterByItemDto.Types.BURN
        )
    }

    @Test
    fun `eth nft activities item by user type`() {
        val result = ethActivityConverter.convertToNftUserTypes(
            // To check deduplication
            UserActivityTypeDto.values().toList() + UserActivityTypeDto.values().toList()
        )

        assertThat(result).hasSize(4)
        assertThat(result).contains(
            NftActivityFilterByUserDto.Types.TRANSFER_FROM,
            NftActivityFilterByUserDto.Types.TRANSFER_TO,
            NftActivityFilterByUserDto.Types.MINT,
            NftActivityFilterByUserDto.Types.BURN
        )
    }

    @Test
    fun `eth order activities item by all type`() {
        val result = ethActivityConverter.convertToOrderAllTypes(
            // To check deduplication
            ActivityTypeDto.values().toList() + ActivityTypeDto.values().toList()
        )

        assertThat(result).hasSize(5)
        assertThat(result).contains(
            OrderActivityFilterAllDto.Types.BID,
            OrderActivityFilterAllDto.Types.LIST,
            OrderActivityFilterAllDto.Types.MATCH,
            OrderActivityFilterAllDto.Types.CANCEL_LIST,
            OrderActivityFilterAllDto.Types.CANCEL_BID
        )
    }

    @Test
    fun `eth order activities item by collection type`() {
        val result = ethActivityConverter.convertToOrderCollectionTypes(
            // To check deduplication
            ActivityTypeDto.values().toList() + ActivityTypeDto.values().toList()
        )

        assertThat(result).hasSize(5)
        assertThat(result).contains(
            OrderActivityFilterByCollectionDto.Types.BID,
            OrderActivityFilterByCollectionDto.Types.LIST,
            OrderActivityFilterByCollectionDto.Types.MATCH,
            OrderActivityFilterByCollectionDto.Types.CANCEL_LIST,
            OrderActivityFilterByCollectionDto.Types.CANCEL_BID
        )
    }

    @Test
    fun `eth order activities item by item type`() {
        val result = ethActivityConverter.convertToOrderItemTypes(
            // To check deduplication
            ActivityTypeDto.values().toList() + ActivityTypeDto.values().toList()
        )

        assertThat(result).hasSize(5)
        assertThat(result).contains(
            OrderActivityFilterByItemDto.Types.BID,
            OrderActivityFilterByItemDto.Types.LIST,
            OrderActivityFilterByItemDto.Types.MATCH,
            OrderActivityFilterByItemDto.Types.CANCEL_LIST,
            OrderActivityFilterByItemDto.Types.CANCEL_BID
        )
    }

    @Test
    fun `eth order activities item by user type`() {
        val result = ethActivityConverter.convertToOrderUserTypes(
            // To check deduplication
            UserActivityTypeDto.values().toList() + UserActivityTypeDto.values().toList()
        )

        assertThat(result).hasSize(7)
        assertThat(result).contains(
            OrderActivityFilterByUserDto.Types.MAKE_BID,
            OrderActivityFilterByUserDto.Types.GET_BID,
            OrderActivityFilterByUserDto.Types.BUY,
            OrderActivityFilterByUserDto.Types.LIST,
            OrderActivityFilterByUserDto.Types.SELL,
            OrderActivityFilterByUserDto.Types.CANCEL_BID,
            OrderActivityFilterByUserDto.Types.CANCEL_LIST
        )
    }
}
