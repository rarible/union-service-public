package com.rarible.protocol.union.api.controller.test.mock.tezos

import com.rarible.protocol.tezos.api.client.NftOwnershipControllerApi
import com.rarible.protocol.tezos.dto.NftOwnershipDto
import com.rarible.protocol.tezos.dto.NftOwnershipsDto
import com.rarible.protocol.union.core.util.CompositeItemIdParser

import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import io.mockk.every
import reactor.core.publisher.Mono

class TezosOwnershipControllerApiMock(
    private val nftOwnershipControllerApi: NftOwnershipControllerApi
) {

    fun mockGetNftOwnershipById(ownershipId: OwnershipIdDto, returnOwnership: NftOwnershipDto?) {
        every {
            nftOwnershipControllerApi.getNftOwnershipById(ownershipId.value)
        } returns (if (returnOwnership == null) Mono.empty() else Mono.just(returnOwnership))
    }

    fun mockGetNftOwnershipsByItem(
        itemId: ItemIdDto,
        continuation: String?,
        size: Int?,
        vararg returnOwnerships: NftOwnershipDto
    ) {
        val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
        every {
            nftOwnershipControllerApi.getNftOwnershipByItem(
                contract,
                tokenId.toString(),
                size,
                continuation
            )
        } returns Mono.just(NftOwnershipsDto(returnOwnerships.size, null, returnOwnerships.asList()))
    }

    fun mockGetNftAllOwnerships(continuation: String?, size: Int, vararg returnOwnerships: NftOwnershipDto) {
        every {
            nftOwnershipControllerApi.getNftAllOwnerships(
                size,
                continuation
            )
        } returns Mono.just(NftOwnershipsDto(returnOwnerships.size, null, returnOwnerships.asList()))
    }

}