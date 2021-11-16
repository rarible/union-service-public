package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.dto.NftItemDeleteEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthItemConverter
import org.slf4j.LoggerFactory

abstract class EthItemEventHandler(
    blockchain: BlockchainDto,
    override val handler: IncomingEventHandler<UnionItemEvent>
) : AbstractBlockchainEventHandler<NftItemEventDto, UnionItemEvent>(blockchain) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun handleInternal(event: NftItemEventDto) {
        logger.debug("Received Ethereum ({}) Item event: type={}", blockchain, event::class.java.simpleName)

        when (event) {
            is NftItemUpdateEventDto -> {
                val item = EthItemConverter.convert(event.item, blockchain)
                handler.onEvent(UnionItemUpdateEvent(item))
            }
            is NftItemDeleteEventDto -> {
                val itemId = ItemIdDto(
                    blockchain = blockchain,
                    contract = EthConverter.convert(event.item.token),
                    tokenId = event.item.tokenId
                )
                handler.onEvent(UnionItemDeleteEvent(itemId))
            }
        }
    }
}

open class EthereumItemEventHandler(
    handler: IncomingEventHandler<UnionItemEvent>
) : EthItemEventHandler(BlockchainDto.ETHEREUM, handler) {
    @CaptureTransaction("ItemEvent#ETHEREUM")
    override suspend fun handle(event: NftItemEventDto) = handleInternal(event)
}

open class PolygonItemEventHandler(
    handler: IncomingEventHandler<UnionItemEvent>
) : EthItemEventHandler(BlockchainDto.POLYGON, handler) {
    @CaptureTransaction("ItemEvent#POLYGON")
    override suspend fun handle(event: NftItemEventDto) = handleInternal(event)
}