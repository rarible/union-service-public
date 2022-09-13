package com.rarible.protocol.union.integration.tezos.mock

import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.tzkt.client.TokenClient
import com.rarible.tzkt.model.Page
import com.rarible.tzkt.model.Token
import io.mockk.coEvery

class TezosItemControllerApiMock(
    private val nftItemControllerApi: TokenClient
) {

    fun mockGetNftItemById(itemId: ItemIdDto, returnItem: Token) {
        coEvery {
            nftItemControllerApi.token(itemId.value, any(), any())
        } returns returnItem
    }

    fun mockGetNftOrderItemsByOwner(
        owner: String,
        continuation: String?,
        size: Int,
        vararg returnItems: Token
    ) {
        coEvery {
            nftItemControllerApi.tokensByOwner(owner, any(), any())
        } returns Page(returnItems.asList(), null)
    }

    fun mockGetNftOrderItemsByCollection(
        collection: String,
        continuation: String?,
        size: Int,
        vararg returnItems: Token
    ) {
        coEvery {
            nftItemControllerApi.tokensByCollection(collection, any(), any())
        } returns Page(returnItems.asList(), null)
    }

    fun mockGetNftOrderItemsByCreator(
        creator: String,
        continuation: String?,
        size: Int,
        vararg returnItems: Token
    ) {
        coEvery {
            nftItemControllerApi.tokensByCreator(creator, any(), any())
        } returns Page(returnItems.asList(), null)
    }
}
