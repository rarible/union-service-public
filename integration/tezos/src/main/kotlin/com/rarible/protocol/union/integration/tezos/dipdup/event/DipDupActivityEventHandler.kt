package com.rarible.protocol.union.integration.tezos.dipdup.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.dipdup.client.core.model.*
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
            if (skip(event) || skipByType(event)) {
                logger.warn("Activity event was skipped")
            } else {
                val unionEvent = dipDupOrderConverter.convert(event, blockchain)
                handler.onEvent(unionEvent)

                if (!properties.useDipDupTokens && dipDupTransfersEventHandler.isTransfersEvent(unionEvent)) {
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

    // We skip fxhash because it's not supported yet
    private fun skipByType(activity: DipDupActivity): Boolean {
        return when(activity) {
            is DipDupOrderListActivity -> activity.source == TezosPlatform.FXHASH_V1 || activity.source == TezosPlatform.FXHASH_V2
            is DipDupOrderCancelActivity -> activity.source == TezosPlatform.FXHASH_V1 || activity.source == TezosPlatform.FXHASH_V2
            is DipDupOrderSellActivity -> activity.source == TezosPlatform.FXHASH_V1 || activity.source == TezosPlatform.FXHASH_V2
            else -> false
        }
    }

}
