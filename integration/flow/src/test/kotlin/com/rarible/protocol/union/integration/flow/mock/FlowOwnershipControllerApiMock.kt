package com.rarible.protocol.union.integration.flow.mock

import com.rarible.protocol.dto.FlowNftOwnershipDto
import com.rarible.protocol.dto.FlowNftOwnershipsDto
import com.rarible.protocol.flow.nft.api.client.FlowNftOwnershipControllerApi
import com.rarible.protocol.union.core.util.CompositeItemIdParser
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
        val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
        every {
            nftOwnershipControllerApi.getNftOwnershipsByItem(
                contract,
                tokenId.toString(),
                continuation,
                size
            )
        } returns Mono.just(FlowNftOwnershipsDto(null, returnOwnerships.asList()))
    }

    fun mockGetNftAllOwnerships(continuation: String?, size: Int, vararg returnOwnerships: FlowNftOwnershipDto) {
        every {
            nftOwnershipControllerApi.getNftAllOwnerships(
                continuation,
                size
            )
        } returns Mono.just(FlowNftOwnershipsDto(null, returnOwnerships.asList()))
    }

}