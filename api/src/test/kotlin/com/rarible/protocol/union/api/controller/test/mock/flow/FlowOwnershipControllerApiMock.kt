package com.rarible.protocol.union.api.controller.test.mock.flow

import com.rarible.protocol.dto.FlowNftOwnershipDto
import com.rarible.protocol.dto.FlowNftOwnershipsDto
import com.rarible.protocol.flow.nft.api.client.FlowNftOwnershipControllerApi
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import io.mockk.every
import reactor.core.publisher.Mono

class FlowOwnershipControllerApiMock(
    private val nftOwnershipControllerApi: FlowNftOwnershipControllerApi
) {

    fun mockGetNftOwnershipById(ownershipId: OwnershipIdDto, returnOwnership: FlowNftOwnershipDto?) {
        every {
            nftOwnershipControllerApi.getNftOwnershipById(ownershipId.value)
        } returns (if (returnOwnership == null) Mono.empty() else Mono.just(returnOwnership))
    }

    fun mockGetNftOwnershipsByItem(
        itemId: ItemIdDto,
        continuation: String?,
        size: Int?,
        vararg returnOwnerships: FlowNftOwnershipDto
    ) {
        every {
            nftOwnershipControllerApi.getNftOwnershipsByItem(
                itemId.token.value,
                itemId.tokenId.toString(),
                continuation,
                size
            )
        } returns Mono.just(FlowNftOwnershipsDto(returnOwnerships.size.toLong(), null, returnOwnerships.asList()))
    }

    fun mockGetNftAllOwnerships(continuation: String?, size: Int, vararg returnOwnerships: FlowNftOwnershipDto) {
        every {
            nftOwnershipControllerApi.getNftAllOwnerships(
                continuation,
                size
            )
        } returns Mono.just(FlowNftOwnershipsDto(returnOwnerships.size.toLong(), null, returnOwnerships.asList()))
    }

}