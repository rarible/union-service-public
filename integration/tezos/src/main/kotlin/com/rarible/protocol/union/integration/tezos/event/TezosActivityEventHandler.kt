package com.rarible.protocol.union.integration.tezos.event

import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.tezos.converter.TezosActivityConverter
import org.slf4j.LoggerFactory

class TezosActivityEventHandler(
    override val handler: IncomingEventHandler<ActivityDto>,
    private val tezosActivityConverter: TezosActivityConverter
) : BlockchainEventHandler<com.rarible.protocol.tezos.dto.ActivityDto, ActivityDto>(BlockchainDto.TEZOS) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: com.rarible.protocol.tezos.dto.ActivityDto) {
        logger.debug("Received Tezos ({}) Activity event: type={}", blockchain, event::class.java.simpleName)
        // if type == null, it means event unparseable - will be logged inside of parser
        if (event.type != null) {
            val unionEventDto = tezosActivityConverter.convert(event.type!!, blockchain)
            handler.onEvent(unionEventDto)
        }
    }
}
