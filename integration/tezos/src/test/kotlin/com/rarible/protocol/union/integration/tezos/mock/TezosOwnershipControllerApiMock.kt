package com.rarible.protocol.union.integration.tezos.mock

import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.tzkt.client.OwnershipClient
import com.rarible.tzkt.model.Page
import com.rarible.tzkt.model.TokenBalance
import io.mockk.coEvery

class TezosOwnershipControllerApiMock(
    private val nftOwnershipControllerApi: OwnershipClient
) {

    fun mockGetNftOwnershipById(ownershipId: OwnershipIdDto, returnOwnership: TokenBalance) {
        coEvery {
            nftOwnershipControllerApi.ownershipById(ownershipId.value)
        } returns returnOwnership
    }

    fun mockGetNftOwnershipsByIds(ownershipIds: List<OwnershipIdDto>, returnOwnerships: List<TokenBalance>) {
        coEvery {
            nftOwnershipControllerApi.ownershipsByIds(ownershipIds.map { it.value })
        } returns returnOwnerships
    }

    fun mockGetNftOwnershipsByItem(
        itemId: ItemIdDto,
        continuation: String?,
        size: Int?,
        vararg returnOwnerships: TokenBalance
    ) {
        coEvery {
            nftOwnershipControllerApi.ownershipsByToken(
                itemId.value,
                size!!,
                continuation,
                any()
            )
        } returns Page(returnOwnerships.asList(), null)
    }

    fun mockGetNftAllOwnerships(continuation: String?, size: Int, vararg returnOwnerships: TokenBalance) {
        coEvery {
            nftOwnershipControllerApi.ownershipsAll(
                any(),
                size
            )
        } returns Page(returnOwnerships.asList(), null)
    }
}
