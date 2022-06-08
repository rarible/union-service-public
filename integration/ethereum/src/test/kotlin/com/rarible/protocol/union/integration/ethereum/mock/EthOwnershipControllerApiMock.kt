package com.rarible.protocol.union.integration.ethereum.mock

import com.rarible.protocol.dto.NftOwnershipDto
import com.rarible.protocol.dto.NftOwnershipsDto
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.union.core.util.CompositeItemIdParser
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
            nftOwnershipControllerApi.getNftOwnershipById(ownershipId.value, false)
        } returns (if (returnOwnership == null) Mono.empty() else Mono.just(returnOwnership))
    }

    fun mockGetNftOwnershipById(ownershipId: String, returnOwnership: NftOwnershipDto?) {
        every {
            nftOwnershipControllerApi.getNftOwnershipById(ownershipId, false)
        } returns (if (returnOwnership == null) Mono.empty() else Mono.just(returnOwnership))
    }

    fun mockGetNftOwnershipByIdNotFound(ownershipId: OwnershipIdDto) {
        every {
            nftOwnershipControllerApi.getNftOwnershipById(ownershipId.value, false)
        } throws WebClientResponseException(404, "", null, null, null, null)
    }

    fun mockGetNftOwnershipsByItem(
        itemId: ItemIdDto,
        continuation: String?,
        size: Int?,
        vararg returnOwnerships: NftOwnershipDto
    ) {
        val (contract, tokenId) = CompositeItemIdParser.split(itemId.value)
        every {
            nftOwnershipControllerApi.getNftOwnershipsByItem(
                contract,
                tokenId.toString(),
                continuation,
                size
            )
        } returns Mono.just(NftOwnershipsDto(returnOwnerships.size.toLong(), null, returnOwnerships.asList()))
    }

    fun mockGetNftOwnershipsByOwner(
        owner: String,
        continuation: String?,
        size: Int?,
        returnOwnerships: List<NftOwnershipDto>
    ) {
        every {
            nftOwnershipControllerApi.getNftOwnershipsByOwner(
                owner,
                continuation,
                size
            )
        } returns Mono.just(NftOwnershipsDto(returnOwnerships.size.toLong(), null, returnOwnerships))
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
