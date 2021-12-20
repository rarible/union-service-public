package com.rarible.protocol.union.integration.tezos.event

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.tezos.api.ApiClient
import com.rarible.protocol.tezos.dto.TezosActivitySafeDto
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.tezos.converter.TezosActivityConverter
import org.slf4j.LoggerFactory

open class TezosActivityEventHandler(
    override val handler: IncomingEventHandler<ActivityDto>,
    private val tezosActivityConverter: TezosActivityConverter
) : AbstractBlockchainEventHandler<TezosActivitySafeDto, ActivityDto>(BlockchainDto.TEZOS) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val mapper = ApiClient.createDefaultObjectMapper()

    @CaptureTransaction("ActivityEvent#TEZOS")
    override suspend fun handle(event: TezosActivitySafeDto) {
        if (event.nftType != null) {
            logger.info("Received Tezos ({}) Order Activity event: {}", blockchain, mapper.writeValueAsString(event))
            val unionEventDto = tezosActivityConverter.convert(event.nftType!!, blockchain)
            handler.onEvent(unionEventDto)
        } else if (event.orderType != null) {
            logger.info("Received Tezos ({}) Order Activity event: {}", blockchain, mapper.writeValueAsString(event))
            val unionEventDto = tezosActivityConverter.convert(event.orderType!!, blockchain)
            handler.onEvent(unionEventDto)
        }
        // if both types == null, it means event unparseable - will be logged inside of parser
    }
}
