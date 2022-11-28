package com.rarible.protocol.union.integration.tezos.dipdup.event

import com.rarible.dipdup.listener.model.DipDupItemMetaEvent
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemMetaEvent
import com.rarible.protocol.union.core.model.UnionItemMetaRefreshEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import org.slf4j.LoggerFactory

open class DipDupItemMetaEventHandler(
    override val handler: IncomingEventHandler<UnionItemMetaEvent>
) : AbstractBlockchainEventHandler<DipDupItemMetaEvent, UnionItemMetaEvent>(
    BlockchainDto.TEZOS,
    EventType.ITEM_META
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(event: DipDupItemMetaEvent): UnionItemMetaEvent {
        logger.info("Received {} dipdup token meta event: {}", blockchain, event)
        val itemId = ItemIdDto(blockchain, event.itemId)
        return UnionItemMetaRefreshEvent(itemId)
    }
}
