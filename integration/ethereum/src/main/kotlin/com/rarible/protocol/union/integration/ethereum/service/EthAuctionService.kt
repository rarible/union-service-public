package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.dto.AuctionIdsDto
import com.rarible.protocol.order.api.client.AuctionControllerApi
import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.service.AuctionService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionSortDto
import com.rarible.protocol.union.dto.AuctionStatusDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.integration.ethereum.converter.EthAuctionConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst

open class EthAuctionService(
    override val blockchain: BlockchainDto,
    private val auctionControllerApi: AuctionControllerApi,
    private val ethAuctionConverter: EthAuctionConverter
) : AbstractBlockchainService(blockchain), AuctionService {

    override suspend fun getAuctionsByIds(orderIds: List<String>): List<AuctionDto> {
        val orders = auctionControllerApi
            .getAuctionsByIds(AuctionIdsDto(orderIds.map { Word.apply(it) }))
            .collectList().awaitFirst()
        return orders.map { ethAuctionConverter.convert(it, blockchain) }
    }

    override suspend fun getAuctionsAll(
        sort: AuctionSortDto?,
        status: List<AuctionStatusDto>?,
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?
    ): Slice<AuctionDto> {
        val auctions = auctionControllerApi.getAuctionsAll(
            EthConverter.convert(sort),
            EthConverter.convert(status),
            origin,
            EthConverter.convert(platform),
            continuation,
            size
        ).awaitFirst()
        return ethAuctionConverter.convert(auctions, blockchain)
    }

    override suspend fun getAuctionsByItem(
        contract: String,
        tokenId: String,
        seller: String?,
        sort: AuctionSortDto?,
        origin: String?,
        status: List<AuctionStatusDto>?,
        currencyId: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?
    ): Slice<AuctionDto> {
        val auctions = auctionControllerApi.getAuctionsByItem(
            contract,
            tokenId,
            seller,
            EthConverter.convert(sort),
            origin,
            EthConverter.convert(status),
            currencyId,
            EthConverter.convert(platform),
            continuation,
            size
        ).awaitFirst()
        return ethAuctionConverter.convert(auctions, blockchain)
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
