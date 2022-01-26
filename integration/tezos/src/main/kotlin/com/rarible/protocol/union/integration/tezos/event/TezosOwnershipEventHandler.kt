package com.rarible.protocol.union.integration.tezos.event

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.tezos.dto.TezosOwnershipSafeEventDto
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.tezos.converter.TezosOwnershipConverter
import org.slf4j.LoggerFactory

open class TezosOwnershipEventHandler(
    override val handler: IncomingEventHandler<UnionOwnershipEvent>
) : AbstractBlockchainEventHandler<TezosOwnershipSafeEventDto, UnionOwnershipEvent>(BlockchainDto.TEZOS) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @CaptureTransaction("OwnershipEvent#TEZOS")
    override suspend fun handle(event: TezosOwnershipSafeEventDto) {
        logger.info("Received {} Ownership event: type = {}, ownerhips = {}", blockchain, event.type, event.ownership)

        when (event.type) {
            TezosOwnershipSafeEventDto.Type.UPDATE -> {
                val ownership = TezosOwnershipConverter.convert(event.ownership!!, blockchain)
                handler.onEvent(UnionOwnershipUpdateEvent(ownership))
            }
            TezosOwnershipSafeEventDto.Type.DELETE -> {
                val ownership = TezosOwnershipConverter.convert(event.ownership!!, blockchain)
                handler.onEvent(UnionOwnershipDeleteEvent(ownership.id))
            }
            TezosOwnershipSafeEventDto.Type.SERIALIZATION_FAILED -> {
                // skip it, will be logged inside of parser
            }
        }
    }
}
