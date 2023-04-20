package com.rarible.protocol.union.integration.tezos.dipdup.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.dipdup.client.core.model.DipDupActivity
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.exception.UnionDataFormatException
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupIntegrationProperties
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupActivityConverter
import org.slf4j.LoggerFactory

open class DipDupActivityEventHandler(
    override val handler: IncomingEventHandler<UnionActivityDto>,
    private val dipDupOrderConverter: DipDupActivityConverter,
    private val dipDupTransfersEventHandler: DipDupTransfersEventHandler,
    private val properties: DipDupIntegrationProperties,
    private val mapper: ObjectMapper
) : AbstractBlockchainEventHandler<DipDupActivity, UnionActivityDto>(
    BlockchainDto.TEZOS,
    EventType.ACTIVITY
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(event: DipDupActivity): UnionActivityDto? {
        logger.info("Received DipDup activity event: {}", mapper.writeValueAsString(event))
        return try {
            if (properties.useDipDupTokens) {
                val unionEvent = dipDupOrderConverter.convert(event, blockchain)
                unionEvent
            } else {
                val unionEvent = dipDupOrderConverter.convertLegacy(event, blockchain)
                if (dipDupTransfersEventHandler.isTransfersEvent(unionEvent)) {
                    dipDupTransfersEventHandler.handle(unionEvent)
                }
                unionEvent
            }
        } catch (e: UnionDataFormatException) {
            logger.warn("Activity event was skipped because wrong data format", e)
            null
        }
    }
}
