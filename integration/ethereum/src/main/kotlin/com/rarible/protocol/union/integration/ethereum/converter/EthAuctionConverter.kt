package com.rarible.protocol.union.integration.ethereum.converter

import com.rarible.protocol.dto.AuctionsPaginationDto
import com.rarible.protocol.dto.RaribleAuctionV1Dto
import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.AuctionDto
import com.rarible.protocol.union.dto.AuctionHistoryDto
import com.rarible.protocol.union.dto.AuctionIdDto
import com.rarible.protocol.union.dto.AuctionSortDto
import com.rarible.protocol.union.dto.AuctionStatusDto
import com.rarible.protocol.union.dto.BlockchainDto
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
            logger.error("Failed to convert {} Auction: {} \n{}", blockchain, e.message, auction)
            throw e
        }
    }

    suspend fun convert(source: AuctionsPaginationDto, blockchain: BlockchainDto): Slice<AuctionDto> {
        return Slice(
            continuation = source.continuation,
            entities = source.auctions.map { convert(it, blockchain) }
        )
    }

    private suspend fun convertInternal(auction: com.rarible.protocol.dto.AuctionDto, blockchain: BlockchainDto): AuctionDto {
        val auctionId = AuctionIdDto(blockchain, EthConverter.convert(auction.hash))
        val seller = EthConverter.convert(auction.seller, blockchain)
        val sell = EthConverter.convert(auction.sell, blockchain)
        val buy = EthConverter.convert(auction.buy, blockchain)
        val buyPriceUsd = currencyService.toUsd(blockchain, buy, auction.buyPrice)
        return when (auction) {
            is RaribleAuctionV1Dto -> {
                AuctionDto(
                    id = auctionId,
                    type = AuctionDto.Type.RARIBLE_AUCTION_V1,
                    seller = seller,
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
                    lastBid = auction.lastBid?.let { EthConverter.convert(it, blockchain) },
                    data = EthConverter.convert(auction.data, blockchain)
                )
            }
        }
    }

    fun convert(source: List<com.rarible.protocol.dto.AuctionHistoryDto>?): List<AuctionHistoryDto>? {
        return source?.map { AuctionHistoryDto(it.hash.toString()) }
    }

    fun convert(source: com.rarible.protocol.dto.AuctionStatusDto): AuctionStatusDto {
        return when(source) {
            com.rarible.protocol.dto.AuctionStatusDto.ACTIVE -> AuctionStatusDto.ACTIVE
            com.rarible.protocol.dto.AuctionStatusDto.CANCELLED -> AuctionStatusDto.CANCELLED
            com.rarible.protocol.dto.AuctionStatusDto.FINISHED -> AuctionStatusDto.FINISHED
        }
    }

    fun convert(source: com.rarible.protocol.dto.AuctionSortDto): AuctionSortDto {
        return when(source) {
            com.rarible.protocol.dto.AuctionSortDto.LAST_UPDATE_ASC -> AuctionSortDto.LAST_UPDATE_ASC
            com.rarible.protocol.dto.AuctionSortDto.LAST_UPDATE_DESC -> AuctionSortDto.LAST_UPDATE_DESC
            com.rarible.protocol.dto.AuctionSortDto.BUY_PRICE_ASC -> AuctionSortDto.BUY_PRICE_ASC
        }
    }
}

