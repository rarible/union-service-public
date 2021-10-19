package com.rarible.protocol.union.api.controller.test.mock.tezos

import com.rarible.protocol.tezos.api.client.NftItemControllerApi
import com.rarible.protocol.tezos.dto.NftItemDto
import com.rarible.protocol.tezos.dto.NftItemsDto
import com.rarible.protocol.union.api.controller.test.mock.WebClientExceptionMock
import com.rarible.protocol.union.dto.ItemIdDto
import io.mockk.every
import reactor.core.publisher.Mono

class TezosItemControllerApiMock(
    private val nftItemControllerApi: NftItemControllerApi
) {

    fun mockGetNftItemById(itemId: ItemIdDto, returnItem: NftItemDto?) {
        every {
            nftItemControllerApi.getNftItemById(itemId.value, true)
        } returns (if (returnItem == null) Mono.empty() else Mono.just(returnItem))
    }

    fun mockGetNftItemById(itemId: ItemIdDto, status: Int, error: Any) {
        every {
            nftItemControllerApi.getNftItemById(itemId.value, true)
        } throws WebClientExceptionMock.mock(status, error)
    }

    // TODO uncomment when supported
    /*fun mockGetNftAllItems(
        continuation: String?,
        size: Int,
        showDeleted: Boolean,
        lastUpdatedFrom: Long,
        lastUpdatedTo: Long,
        vararg returnItems: NftItemDto
    ) {
        every {
            nftItemControllerApi.getNftAllItems(
                continuation,
                size,
                showDeleted,
                lastUpdatedFrom,
                lastUpdatedTo
            )
        } returns Mono.just(NftItemsDto(returnItems.size, null, returnItems.asList()))
    }*/

    fun mockGetNftOrderItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int,
        vararg returnItems: NftItemDto
    ) {
        every {
            nftItemControllerApi.getNftItemsByOwner(owner, true, size, continuation)
        } returns Mono.just(NftItemsDto(returnItems.size, null, returnItems.asList()))
    }

    fun mockGetNftOrderItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int,
        vararg returnItems: NftItemDto
    ) {
        every {
            nftItemControllerApi.getNftItemsByCollection(collection, true, size, continuation)
        } returns Mono.just(NftItemsDto(returnItems.size, null, returnItems.asList()))
    }

    fun mockGetNftOrderItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int,
        vararg returnItems: NftItemDto
    ) {
        every {
            nftItemControllerApi.getNftItemsByCreator(creator, true, size, continuation)
        } returns Mono.just(NftItemsDto(returnItems.size, null, returnItems.asList()))
    }
}
