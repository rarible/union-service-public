package com.rarible.protocol.union.api.controller.test.mock.flow

import com.rarible.protocol.dto.FlowNftItemDto
import com.rarible.protocol.dto.FlowNftItemsDto
import com.rarible.protocol.flow.nft.api.client.FlowNftItemControllerApi
import com.rarible.protocol.union.api.controller.test.mock.WebClientExceptionMock
import com.rarible.protocol.union.dto.ItemIdDto
import io.mockk.every
import reactor.core.publisher.Mono

class FlowItemControllerApiMock(
    private val nftItemControllerApi: FlowNftItemControllerApi
) {

    fun mockGetNftItemById(itemId: ItemIdDto, returnItem: FlowNftItemDto?) {
        every {
            nftItemControllerApi.getNftItemById(itemId.value)
        } returns (if (returnItem == null) Mono.empty() else Mono.just(returnItem))
    }

    fun mockGetNftItemById(itemId: ItemIdDto, status: Int, error: Any) {
        every {
            nftItemControllerApi.getNftItemById(itemId.value)
        } throws WebClientExceptionMock.mock(status, error)
    }

    fun mockGetNftAllItems(
        continuation: String,
        size: Int,
        showDeleted: Boolean,
        lastUpdatedFrom: Long,
        lastUpdatedTo: Long,
        vararg returnItems: FlowNftItemDto
    ) {
        every {
            nftItemControllerApi.getNftAllItems(
                continuation,
                size,
                showDeleted
            )
        } returns Mono.just(FlowNftItemsDto(returnItems.size.toLong(), null, returnItems.asList()))
    }

    fun mockGetNftOrderItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int,
        vararg returnItems: FlowNftItemDto
    ) {
        every {
            nftItemControllerApi.getNftItemsByOwner(owner, continuation, size)
        } returns Mono.just(FlowNftItemsDto(returnItems.size.toLong(), null, returnItems.asList()))
    }

    fun mockGetNftOrderItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int,
        vararg returnItems: FlowNftItemDto
    ) {
        every {
            nftItemControllerApi.getNftItemsByCollection(collection, continuation, size)
        } returns Mono.just(FlowNftItemsDto(returnItems.size.toLong(), null, returnItems.asList()))
    }

    fun mockGetNftOrderItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int,
        vararg returnItems: FlowNftItemDto
    ) {
        every {
            nftItemControllerApi.getNftItemsByCreator(creator, continuation, size)
        } returns Mono.just(FlowNftItemsDto(returnItems.size.toLong(), null, returnItems.asList()))
    }

}
