package com.rarible.protocol.union.integration.tezos.dipdup.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.apm.CaptureTransaction
import com.rarible.dipdup.listener.model.DipDupDeleteItemEvent
import com.rarible.dipdup.listener.model.DipDupItemEvent
import com.rarible.dipdup.listener.model.DipDupUpdateItemEvent
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupItemConverter
import org.slf4j.LoggerFactory

open class DipDupItemEventHandler(
    override val handler: IncomingEventHandler<UnionItemEvent>,
    private val mapper: ObjectMapper
) : AbstractBlockchainEventHandler<DipDupItemEvent, UnionItemEvent>(
    BlockchainDto.TEZOS
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @CaptureTransaction("ItemEvent#TEZOS")
    override suspend fun handle(event: DipDupItemEvent) = handler.onEvent(convert(event))

    @CaptureTransaction("ItemEvents#TEZOS")
    override suspend fun handle(events: List<DipDupItemEvent>) = handler.onEvents(events.map { convert(it) })

    private fun convert(event: DipDupItemEvent): UnionItemEvent {
        logger.info("Received DipDup item event: {}", mapper.writeValueAsString(event))
        return when (event) {
            is DipDupUpdateItemEvent -> {
                val item = DipDupItemConverter.convert(event.item)
                UnionItemUpdateEvent(item)
            }
            is DipDupDeleteItemEvent -> {
                val itemId = ItemIdDto(blockchain, event.itemId)
                UnionItemDeleteEvent(itemId)
            }
        }
    }
}
