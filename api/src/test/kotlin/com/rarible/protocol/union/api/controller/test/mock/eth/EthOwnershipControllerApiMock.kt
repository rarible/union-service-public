package com.rarible.protocol.union.api.controller.test.mock.eth

import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.dto.NftOwnershipsDto
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import io.mockk.every
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono

class EthOwnershipControllerApiMock(
    private val nftOwnershipControllerApi: NftOwnershipControllerApi
) {

    fun mockGetNftOwnershipById(ownershipId: OwnershipIdDto, returnOwnership: NftOwnershipDto?) {
        every {
            nftOwnershipControllerApi.getNftOwnershipById(ownershipId.value)
        } returns (if (returnOwnership == null) Mono.empty() else Mono.just(returnOwnership))
    }

    fun mockGetNftOwnershipByIdNotFound(ownershipId: OwnershipIdDto) {
        every {
            nftOwnershipControllerApi.getNftOwnershipById(ownershipId.value)
        } throws WebClientResponseException(404, "", null, null, null, null)
    }

    fun mockGetNftOwnershipsByItem(
        itemId: ItemIdDto,
        continuation: String?,
        size: Int?,
        vararg returnOwnerships: NftOwnershipDto
    ) {
        every {
            nftOwnershipControllerApi.getNftOwnershipsByItem(
                itemId.contract,
                itemId.tokenId.toString(),
                continuation,
                size
            )
        } returns Mono.just(NftOwnershipsDto(returnOwnerships.size.toLong(), null, returnOwnerships.asList()))
    }

    fun mockGetNftAllOwnerships(continuation: String?, size: Int, vararg returnOwnerships: NftOwnershipDto) {
        every {
            nftOwnershipControllerApi.getNftAllOwnerships(
                continuation,
                size,
                false
            )
        } returns Mono.just(NftOwnershipsDto(returnOwnerships.size.toLong(), null, returnOwnerships.asList()))
    }

}