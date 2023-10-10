package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.protocol.dto.NftItemMetaEventDto
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemMetaEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthItemMetaConverter
import org.slf4j.LoggerFactory

class EthItemMetaEventHandler(
    blockchain: BlockchainDto,
    override val handler: IncomingEventHandler<UnionItemMetaEvent>
) : AbstractBlockchainEventHandler<NftItemMetaEventDto, UnionItemMetaEvent>(
    blockchain,
    EventType.ITEM
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(event: NftItemMetaEventDto): UnionItemMetaEvent {
        logger.info("Received {} item meta event {}", blockchain, event)
        return EthItemMetaConverter.convert(event, blockchain)
    }
}
