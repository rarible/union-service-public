package com.rarible.protocol.nftorder.api.test.mock

import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemsDto
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.union.api.controller.test.mock.WebClientExceptionMock
import com.rarible.protocol.union.dto.ItemIdDto
import io.mockk.every
import reactor.core.publisher.Mono

class EthItemControllerApiMock(
    private val nftItemControllerApi: NftItemControllerApi
) {

    fun mockGetNftItemById(itemId: ItemIdDto, returnItem: NftItemDto?) {
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
        } returns Mono.just(NftItemsDto(returnItems.size.toLong(), null, returnItems.asList()))
    }

    fun mockGetNftOrderItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int,
        vararg returnItems: NftItemDto
    ) {
        every {
            nftItemControllerApi.getNftItemsByOwner(owner, continuation, size)
        } returns Mono.just(NftItemsDto(returnItems.size.toLong(), null, returnItems.asList()))
    }

    fun mockGetNftOrderItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int,
        vararg returnItems: NftItemDto
    ) {
        every {
            nftItemControllerApi.getNftItemsByCollection(collection, continuation, size)
        } returns Mono.just(NftItemsDto(returnItems.size.toLong(), null, returnItems.asList()))
    }

    fun mockGetNftOrderItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int,
        vararg returnItems: NftItemDto
    ) {
        every {
            nftItemControllerApi.getNftItemsByCreator(creator, continuation, size)
        } returns Mono.just(NftItemsDto(returnItems.size.toLong(), null, returnItems.asList()))
    }
}
