package com.rarible.protocol.union.integration.tezos.mock

import com.rarible.protocol.union.core.util.CompositeItemIdParser
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

    fun mockGetNftOwnershipsByItem(
        itemId: ItemIdDto,
        continuation: String?,
        size: Int?,
        vararg returnOwnerships: TokenBalance
    ) {
        val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
        coEvery {
            nftOwnershipControllerApi.ownershipsByToken(
                contract,
                any(),
                any(),
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
