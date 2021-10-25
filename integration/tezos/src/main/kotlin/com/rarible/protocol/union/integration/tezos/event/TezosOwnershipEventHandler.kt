package com.rarible.protocol.union.integration.tezos.event

import com.rarible.protocol.tezos.dto.OwnershipEventDto
import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.tezos.converter.TezosOwnershipConverter
import org.slf4j.LoggerFactory

class TezosOwnershipEventHandler(
    override val handler: IncomingEventHandler<UnionOwnershipEvent>
) : BlockchainEventHandler<OwnershipEventDto, UnionOwnershipEvent>(BlockchainDto.TEZOS) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: OwnershipEventDto) {
        logger.info("Received Tezos Ownership event: type={}", event::class.java.simpleName)

        when (event.type) {
            OwnershipEventDto.Type.UPDATE -> {
                val ownership = TezosOwnershipConverter.convert(event.ownership!!, blockchain)
                handler.onEvent(UnionOwnershipUpdateEvent(ownership))
            }
            OwnershipEventDto.Type.DELETE -> {
                val ownership = TezosOwnershipConverter.convert(event.ownership!!, blockchain)
                handler.onEvent(UnionOwnershipDeleteEvent(ownership.id))
            }
            OwnershipEventDto.Type.SERIALIZATION_FAILED -> {
                // skip it, will be logged inside of parser
            }
        }
    }
}
