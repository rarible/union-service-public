package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthItemConverter
import org.slf4j.LoggerFactory

class EthItemEventHandler(
    blockchain: BlockchainDto,
    override val handler: IncomingEventHandler<UnionItemEvent>
) : AbstractBlockchainEventHandler<NftItemEventDto, UnionItemEvent>(
    blockchain,
    EventType.ITEM
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(event: NftItemEventDto): UnionItemEvent {
        logger.info("Received {} Item event {}", blockchain, event)
        return EthItemConverter.convert(event, blockchain)
    }
}
