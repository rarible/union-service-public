package com.rarible.protocol.union.integration.ethereum.mock

import com.rarible.protocol.dto.NftItemDto
import com.rarible.protocol.dto.NftItemIdsDto
import com.rarible.protocol.dto.NftItemMetaDto
import com.rarible.protocol.dto.NftItemsDto
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.union.dto.ItemIdDto
import io.mockk.every
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

class EthItemControllerApiMock(
    private val nftItemControllerApi: NftItemControllerApi
) {

    fun mockGetNftItemById(itemId: ItemIdDto, returnItem: NftItemDto?) {
        every {
            nftItemControllerApi.getNftItemById(itemId.value)
        } returns (if (returnItem == null) Mono.empty() else Mono.just(returnItem))
    }

    fun mockGetNftItemMetaById(itemId: ItemIdDto, meta: NftItemMetaDto) {
        every { nftItemControllerApi.getNftItemMetaById(itemId.value) } returns meta.toMono()
    }

    fun mockGetNftItemById(itemId: ItemIdDto, status: Int, error: Any) {
        every {
            nftItemControllerApi.getNftItemById(itemId.value)
        } throws WebClientExceptionMock.mock(status, error)
    }

    fun mockGetNftItemsByIds(ids: List<String>, returnItems: List<NftItemDto>) {
        every {
            nftItemControllerApi.getNftItemsByIds(NftItemIdsDto(ids))
        } returns Flux.fromIterable(returnItems)
    }

    fun mockGetNftAllItems(
        continuation: String?,
        size: Int,
        showDeleted: Boolean,
        lastUpdatedFrom: Long? = null,
        lastUpdatedTo: Long? = null,
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

    fun mockGetNftItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int,
        vararg returnItems: NftItemDto
    ) {
        every {
            nftItemControllerApi.getNftItemsByCollection(collection, any(), continuation, size)
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
