package com.rarible.protocol.nftorder.api.test.mock

import com.rarible.protocol.dto.AuctionDto
import com.rarible.protocol.dto.AuctionIdsDto
import com.rarible.protocol.dto.AuctionsPaginationDto
import com.rarible.protocol.order.api.client.AuctionControllerApi
import com.rarible.protocol.union.dto.ItemIdDto
import io.daonomic.rpc.domain.Word
import io.mockk.every
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class EthAuctionControllerApiMock(
    private val auctionControllerApi: AuctionControllerApi
) {
    fun mockGetAuctionByItem(itemId: ItemIdDto, returnItems: List<AuctionDto>?) {
        every {
            auctionControllerApi.getAuctionsByItem(itemId.contract, itemId.tokenId.toString(), any(), any(), any(), any(), any(), any(), any(), any())
        } returns (if (returnItems == null) Mono.empty() else Mono.just(AuctionsPaginationDto(returnItems, null)))
    }

    fun mockGetAuctionsByIds(ids: List<Word>, returnItems: List<AuctionDto>) {
        every {
            auctionControllerApi.getAuctionsByIds(AuctionIdsDto(ids))
        } returns Flux.fromIterable(returnItems)
    }

}
