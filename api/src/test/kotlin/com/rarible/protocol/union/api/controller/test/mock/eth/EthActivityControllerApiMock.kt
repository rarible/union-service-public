package com.rarible.protocol.union.api.controller.test.mock.eth

import com.rarible.protocol.dto.ActivitySortDto
import com.rarible.protocol.dto.NftActivitiesDto
import com.rarible.protocol.dto.NftActivityDto
import com.rarible.protocol.dto.NftActivityFilterByItemDto
import com.rarible.protocol.dto.OrderActivitiesDto
import com.rarible.protocol.dto.OrderActivityDto
import com.rarible.protocol.dto.OrderActivityFilterByItemDto
import com.rarible.protocol.nft.api.client.NftActivityControllerApi
import com.rarible.protocol.order.api.client.OrderActivityControllerApi
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.dto.ItemIdDto
import io.mockk.coEvery
import reactor.kotlin.core.publisher.toMono
import scalether.domain.Address

class EthActivityControllerApiMock(
    private val ethereumActivityItemApi: NftActivityControllerApi,
    private val ethereumActivityOrderApi: OrderActivityControllerApi
) {

    fun mockGetNftActivitiesByItem(
        itemId: ItemIdDto,
        types: List<NftActivityFilterByItemDto.Types>,
        size: Int,
        continuation: String? = null,
        sort: ActivitySortDto = ActivitySortDto.EARLIEST_FIRST,
        vararg result: NftActivityDto
    ) {
        val (token, tokenId) = CompositeItemIdParser.split(itemId.value)
        val filter = NftActivityFilterByItemDto(
            contract = Address.apply(token),
            tokenId = tokenId,
            types = types
        )
        coEvery {
            ethereumActivityItemApi.getNftActivities(filter, continuation, size, sort)
        } returns NftActivitiesDto(null, result.toList()).toMono()
    }

    fun mockGetOrderActivitiesByItem(
        itemId: ItemIdDto,
        types: List<OrderActivityFilterByItemDto.Types>,
        size: Int,
        continuation: String? = null,
        sort: ActivitySortDto = ActivitySortDto.EARLIEST_FIRST,
        vararg result: OrderActivityDto?
    ) {
        val (token, tokenId) = CompositeItemIdParser.split(itemId.value)
        val filter = OrderActivityFilterByItemDto(
            contract = Address.apply(token),
            tokenId = tokenId,
            types = types
        )
        coEvery {
            ethereumActivityOrderApi.getOrderActivities(filter, continuation, size, sort)
        } returns OrderActivitiesDto(null, result.toList().filterNotNull()).toMono()
    }
}