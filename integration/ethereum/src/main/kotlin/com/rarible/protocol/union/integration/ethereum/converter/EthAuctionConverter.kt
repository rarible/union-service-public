package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.protocol.dto.RaribleAuctionV1Dto
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionHistoryDto
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.AuctionStatusDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.RaribleAuctionV1BidDataV1Dto
import com.rarible.protocol.union.dto.RaribleAuctionV1BidV1Dto
import com.rarible.protocol.union.dto.RaribleAuctionV1DataV1Dto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class EthAuctionConverter(
    private val currencyService: CurrencyService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun convert(auction: com.rarible.protocol.dto.AuctionDto, blockchain: BlockchainDto): AuctionDto {
        try {
            return convertInternal(auction, blockchain)
        } catch (e: Exception) {
            logger.error("Failed to convert Ethereum Auction, cause: {} \n{}", e.message, auction)
            throw e
        }
    }

    private suspend fun convertInternal(auction: com.rarible.protocol.dto.AuctionDto, blockchain: BlockchainDto): AuctionDto {
        val auctionId = AuctionIdDto(blockchain, EthConverter.convert(auction.hash))
        val seller = EthConverter.convert(auction.seller, blockchain)
        val buyer = auction.buyer?.let { EthConverter.convert(it, blockchain) }
        val sell = EthConverter.convert(auction.sell, blockchain)
        val buy = EthConverter.convert(auction.buy, blockchain)
        val buyPriceUsd = currencyService.toUsd(blockchain, buy, auction.buyPrice)
        return when (auction) {
            is RaribleAuctionV1Dto -> {
                AuctionDto(
                    id = auctionId,
                    type = AuctionDto.Type.RARIBLE_AUCTION_V1,
                    seller = seller,
                    buyer = buyer,
                    sell = sell,
                    buy = buy,
                    endTime = auction.endTime,
                    minimalStep = auction.minimalStep,
                    minimalPrice = auction.minimalPrice,
                    createdAt = auction.createdAt,
                    lastUpdateAt = auction.lastUpdateAt,
                    buyPrice = auction.buyPrice,
                    buyPriceUsd = buyPriceUsd,
                    pending = convert(auction.pending),
                    status = convert(auction.status),
                    hash = auction.hash.toString(),
                    auctionId = auction.auctionId,
                    lastBid = convertBid(auction.lastBid, blockchain),
                    data = convert(auction.data, blockchain)
                )
            }
        }
    }

    suspend fun convert(source: List<com.rarible.protocol.dto.AuctionHistoryDto>?): List<AuctionHistoryDto>? {
        return source?.map { AuctionHistoryDto(it.hash.toString()) }
    }

    suspend fun convert(source: com.rarible.protocol.dto.AuctionStatusDto): AuctionStatusDto {
        return when(source) {
            com.rarible.protocol.dto.AuctionStatusDto.ACTIVE -> AuctionStatusDto.ACTIVE
            com.rarible.protocol.dto.AuctionStatusDto.CANCELLED -> AuctionStatusDto.CANCELLED
            com.rarible.protocol.dto.AuctionStatusDto.FINISHED -> AuctionStatusDto.FINISHED
        }
    }

    suspend fun convert(source: com.rarible.protocol.dto.RaribleAuctionV1DataV1Dto, blockchain: BlockchainDto): RaribleAuctionV1DataV1Dto {
        return RaribleAuctionV1DataV1Dto(
            originFees = source.originFees.map { EthConverter.convertToPayout(it, blockchain) },
            payouts = source.payouts.map { EthConverter.convertToPayout(it, blockchain) },
            startTime = source.startTime,
            duration = source.duration,
            buyOutPrice = source.buyOutPrice
        )
    }

    suspend fun convertBid(source: com.rarible.protocol.dto.RaribleAuctionV1BidV1Dto?, blockchain: BlockchainDto): RaribleAuctionV1BidV1Dto? {
        return source?.let { bid ->
            RaribleAuctionV1BidV1Dto(
                amount = bid.amount,
                data = RaribleAuctionV1BidDataV1Dto(
                    originFees = bid.data.originFees.map { EthConverter.convertToPayout(it, blockchain) },
                    payouts = bid.data.payouts.map { EthConverter.convertToPayout(it, blockchain) }
                )
            )
        }
    }
}

