package com.rarible.protocol.union.integration.tezos.dipdup.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.dipdup.client.core.model.DipDupActivity
import com.rarible.protocol.union.core.exception.UnionDataFormatException
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupIntegrationProperties
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupActivityConverter
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit

open class DipDupActivityEventHandler(
    override val handler: IncomingEventHandler<ActivityDto>,
    private val dipDupOrderConverter: DipDupActivityConverter,
    private val dipDupTransfersEventHandler: DipDupTransfersEventHandler,
    private val properties: DipDupIntegrationProperties,
    private val mapper: ObjectMapper
) : AbstractBlockchainEventHandler<DipDupActivity, ActivityDto>(com.rarible.protocol.union.dto.BlockchainDto.TEZOS) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: DipDupActivity) {
        logger.info("Received DipDup activity event: {}", mapper.writeValueAsString(event))
        try {
            if (skip(event)) {
                logger.warn("Activity event was skipped because activity is too old")
            } else {
                val unionEvent = dipDupOrderConverter.convert(event, blockchain)
                handler.onEvent(unionEvent)

                if (dipDupTransfersEventHandler.isTransfersEvent(unionEvent)) {
                    dipDupTransfersEventHandler.handle(unionEvent)
                }
            }
        } catch (e: UnionDataFormatException) {
            logger.warn("Activity event was skipped because wrong data format", e)
        }
    }

    private fun skip(activity: DipDupActivity): Boolean {
        val ignoreDate = OffsetDateTime.now().minus(properties.tzktProperties.ignorePeriod, ChronoUnit.MILLIS)
        return activity.date < ignoreDate
    }

}
