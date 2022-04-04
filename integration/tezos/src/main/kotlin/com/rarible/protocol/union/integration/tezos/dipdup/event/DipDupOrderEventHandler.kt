package com.rarible.protocol.union.integration.tezos.dipdup.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.dipdup.client.core.model.DipDupOrder
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOrderEvent
import com.rarible.protocol.union.core.model.UnionOrderUpdateEvent
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupOrderConverter
import org.slf4j.LoggerFactory

open class DipDupOrderEventHandler(
    override val handler: IncomingEventHandler<UnionOrderEvent>,
    private val dipDupOrderConverter: DipDupOrderConverter,
    private val mapper: ObjectMapper
) : AbstractBlockchainEventHandler<DipDupOrder, UnionOrderEvent>(com.rarible.protocol.union.dto.BlockchainDto.TEZOS) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: DipDupOrder) {
        logger.info("Received DipDup order event: {}", mapper.writeValueAsString(event))
        val unionOrder = dipDupOrderConverter.convert(event, blockchain)
        handler.onEvent(UnionOrderUpdateEvent(unionOrder))
    }

}
