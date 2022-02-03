package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.dto.AuctionEventDto
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionAuctionDeleteEvent
import com.rarible.protocol.union.core.model.UnionAuctionEvent
import com.rarible.protocol.union.core.model.UnionAuctionUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthAuctionConverter
import org.slf4j.LoggerFactory

abstract class EthAuctionEventHandler(
    blockchain: BlockchainDto,
    override val handler: IncomingEventHandler<UnionAuctionEvent>,
    private val ethActionConverter: EthAuctionConverter
) : AbstractBlockchainEventHandler<AuctionEventDto, UnionAuctionEvent>(blockchain) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun handleInternal(event: AuctionEventDto) {
        logger.info("Received {} Auction event: {}", blockchain, event)

        when (event) {
            is com.rarible.protocol.dto.AuctionUpdateEventDto -> {
                val auction = ethActionConverter.convert(event.auction, blockchain)
                handler.onEvent(UnionAuctionUpdateEvent(auction))
            }
            is com.rarible.protocol.dto.AuctionDeleteEventDto -> {
                val auction = ethActionConverter.convert(event.auction, blockchain)
                handler.onEvent(UnionAuctionDeleteEvent(auction))
            }
        }
    }
}

open class EthereumAuctionEventHandler(
    handler: IncomingEventHandler<UnionAuctionEvent>, ethActionConverter: EthAuctionConverter
) : EthAuctionEventHandler(BlockchainDto.ETHEREUM, handler, ethActionConverter) {
    @CaptureTransaction("AuctionEvent#ETHEREUM")
    override suspend fun handle(event: AuctionEventDto) = handleInternal(event)
}
