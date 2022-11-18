package com.rarible.protocol.union.integration.tezos.dipdup.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.apm.CaptureTransaction
import com.rarible.dipdup.client.core.model.DipDupOrder
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupIntegrationProperties
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupOrderConverter
import org.slf4j.LoggerFactory

open class DipDupOrderEventHandler(
    override val handler: IncomingEventHandler<UnionOrderEvent>,
    private val dipDupOrderConverter: DipDupOrderConverter,
    private val mapper: ObjectMapper,
    marketplaces: DipDupIntegrationProperties.Marketplaces
) : AbstractBlockchainEventHandler<DipDupOrder, UnionOrderEvent>(
    BlockchainDto.TEZOS
) {

    private val enabledPlatforms = marketplaces.getEnabledMarketplaces()

    private val logger = LoggerFactory.getLogger(javaClass)

    @CaptureTransaction("OrderEvent#TEZOS")
    override suspend fun handle(event: DipDupOrder) {
        convert(event)?.let { handler.onEvent(it) }
    }

    @CaptureTransaction("OrderEvents#TEZOS")
    override suspend fun handle(events: List<DipDupOrder>) {
        handler.onEvents(events.mapNotNull { convert(it) })
    }

    private suspend fun convert(event: DipDupOrder): UnionOrderEvent? {
        logger.info("Received DipDup order event: {}", mapper.writeValueAsString(event))
        return if (enabledPlatforms.contains(event.platform)) {
            val unionOrder = dipDupOrderConverter.convert(event, blockchain)
            UnionOrderUpdateEvent(unionOrder)
        } else {
            null
        }
    }

}
