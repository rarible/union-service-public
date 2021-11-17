package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.dto.OrderIdsDto
import com.rarible.protocol.order.api.client.AuctionControllerApi
import com.rarible.protocol.order.api.client.OrderControllerApi
import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.converter.UnionConverter
import com.rarible.protocol.union.core.service.AuctionService
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.integration.ethereum.converter.EthAuctionConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthOrderConverter
import kotlinx.coroutines.reactive.awaitFirst

open class EthAuctionService(
    override val blockchain: BlockchainDto,
    private val auctionControllerApi: AuctionControllerApi,
    private val ethAuctionConverter: EthAuctionConverter
) : AbstractBlockchainService(blockchain), AuctionService {

    override suspend fun getAuctionsByIds(orderIds: List<String>): List<AuctionDto> {
        TODO("Not yet implemented")
    }

}

@CaptureSpan(type = "ext", subtype = "ethereum")
open class EthereumAuctionService(
    auctionControllerApi: AuctionControllerApi,
    ethAuctionConverter: EthAuctionConverter
) : EthAuctionService(
    BlockchainDto.ETHEREUM,
    auctionControllerApi,
    ethAuctionConverter
)

@CaptureSpan(type = "ext", subtype = "polygon")
open class PolygonAuctionService(
    auctionControllerApi: AuctionControllerApi,
    ethAuctionConverter: EthAuctionConverter
) : EthAuctionService(
    BlockchainDto.POLYGON,
    auctionControllerApi,
    ethAuctionConverter
)
