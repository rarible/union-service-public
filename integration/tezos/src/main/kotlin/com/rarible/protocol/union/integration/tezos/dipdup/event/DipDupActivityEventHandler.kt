package com.rarible.protocol.union.integration.tezos.dipdup.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.dipdup.client.core.model.DipDupActivity
import com.rarible.protocol.union.core.exception.UnionDataFormatException
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupActivityConverter
import org.slf4j.LoggerFactory

open class DipDupActivityEventHandler(
    override val handler: IncomingEventHandler<ActivityDto>,
    private val dipDupOrderConverter: DipDupActivityConverter,
    private val dipDupTransfersEventHandler: DipDupTransfersEventHandler,
    private val mapper: ObjectMapper
) : AbstractBlockchainEventHandler<DipDupActivity, ActivityDto>(com.rarible.protocol.union.dto.BlockchainDto.TEZOS) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: DipDupActivity) {
        logger.info("Received DipDup activity event: {}", mapper.writeValueAsString(event))
        try {
            val unionEvent = dipDupOrderConverter.convert(event, blockchain)
            handler.onEvent(unionEvent)

            if (dipDupTransfersEventHandler.isTransfersEvent(unionEvent)) {
                dipDupTransfersEventHandler.handle(unionEvent)
            }
        } catch (e: UnionDataFormatException) {
            logger.warn("Activity event was skipped because wrong data format", e)
        }
    }

}
