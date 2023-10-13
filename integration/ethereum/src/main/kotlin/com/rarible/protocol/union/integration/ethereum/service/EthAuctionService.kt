package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.protocol.dto.AuctionIdsDto
import com.rarible.protocol.order.api.client.AuctionControllerApi
import com.rarible.protocol.union.core.service.AuctionService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.core.util.CompositeItemIdParser
import com.rarible.protocol.union.core.util.safeSplit
import com.rarible.protocol.union.dto.AuctionBidDto
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionSortDto
import com.rarible.protocol.union.dto.AuctionStatusDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.ethereum.EthEvmIntegrationProperties
import com.rarible.protocol.union.integration.ethereum.converter.EthAuctionConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import io.daonomic.rpc.domain.Word
import kotlinx.coroutines.reactive.awaitFirst

class EthAuctionService(
    override val blockchain: BlockchainDto,
    private val auctionControllerApi: AuctionControllerApi,
    private val ethAuctionConverter: EthAuctionConverter,
    private val properties: EthEvmIntegrationProperties,
) : AbstractBlockchainService(blockchain), AuctionService {

    override suspend fun getAuctionsBidsById(
        auctionId: String,
        continuation: String?,
        size: Int?
    ): Slice<AuctionBidDto> {
        val bids = auctionControllerApi.getAuctionBidsByHash(auctionId, continuation, size).awaitFirst()
        return ethAuctionConverter.convert(bids, blockchain)
    }

    override suspend fun getAuctionById(auctionId: String): AuctionDto {
        val auction = auctionControllerApi.getAuctionByHash(auctionId).awaitFirst()
        return ethAuctionConverter.convert(auction, blockchain)
    }

    override suspend fun getAuctionsByIds(auctionIds: List<String>): List<AuctionDto> {
        val auctions = auctionControllerApi
            .getAuctionsByIds(AuctionIdsDto(auctionIds.map { Word.apply(it) }))
            .collectList().awaitFirst()
        return auctions.map { ethAuctionConverter.convert(it, blockchain) }
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

    override suspend fun getAuctionsByCollection(
        contract: String,
        seller: String?,
        origin: String?,
        status: List<AuctionStatusDto>?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?
    ): Slice<AuctionDto> {
        val auctions = auctionControllerApi.getAuctionsByCollection(
            contract,
            seller,
            origin,
            EthConverter.convert(status),
            EthConverter.convert(platform),
            continuation,
            size
        ).awaitFirst()
        return ethAuctionConverter.convert(auctions, blockchain)
    }

    override suspend fun getAuctionsByItem(
        itemId: String,
        seller: String?,
        sort: AuctionSortDto?,
        origin: String?,
        status: List<AuctionStatusDto>?,
        currencyId: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?
    ): Slice<AuctionDto> {
        val (contract, tokenId) = CompositeItemIdParser.split(itemId)
        val auctions = auctionControllerApi.getAuctionsByItem(
            contract,
            tokenId.toString(),
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

    override suspend fun getAuctionsBySeller(
        seller: String,
        status: List<AuctionStatusDto>?,
        origin: String?,
        platform: PlatformDto?,
        continuation: String?,
        size: Int?
    ): Slice<AuctionDto> {
        val auctions = auctionControllerApi.getAuctionsBySeller(
            seller,
            EthConverter.convert(status),
            origin,
            EthConverter.convert(platform),
            continuation,
            size
        ).awaitFirst()
        return ethAuctionConverter.convert(auctions, blockchain)
    }

    override fun getAuctionContracts(): List<String> {
        return safeSplit(properties.auctionContracts)
    }
}
