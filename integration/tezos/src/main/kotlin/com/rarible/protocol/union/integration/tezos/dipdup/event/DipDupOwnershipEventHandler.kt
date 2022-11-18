package com.rarible.protocol.union.integration.tezos.dipdup.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.apm.CaptureTransaction
import com.rarible.dipdup.listener.model.DipDupDeleteOwnershipEvent
import com.rarible.dipdup.listener.model.DipDupOwnershipEvent
import com.rarible.dipdup.listener.model.DipDupUpdateOwnershipEvent
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupOwnershipConverter
import org.slf4j.LoggerFactory
import java.math.BigInteger

open class DipDupOwnershipEventHandler(
    override val handler: IncomingEventHandler<UnionOwnershipEvent>,
    private val mapper: ObjectMapper
) : AbstractBlockchainEventHandler<DipDupOwnershipEvent, UnionOwnershipEvent>(
    BlockchainDto.TEZOS
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @CaptureTransaction("OwnershipEvent#TEZOS")
    override suspend fun handle(event: DipDupOwnershipEvent) = handler.onEvent(convert(event))

    @CaptureTransaction("OwnershipEvents#TEZOS")
    override suspend fun handle(events: List<DipDupOwnershipEvent>) = handler.onEvents(events.map { convert(it) })

    private fun convert(event: DipDupOwnershipEvent): UnionOwnershipEvent {
        logger.info("Received DipDup ownership event: {}", mapper.writeValueAsString(event))
        return when (event) {
            is DipDupUpdateOwnershipEvent -> {
                val ownership = DipDupOwnershipConverter.convert(event.ownership)
                UnionOwnershipUpdateEvent(ownership)
            }
            is DipDupDeleteOwnershipEvent -> {
                val (contract, tokenId, owner) = event.ownershipId.split(":")
                val ownershipId = OwnershipIdDto(
                    blockchain = blockchain,
                    contract = contract,
                    tokenId = BigInteger(tokenId),
                    owner = owner
                )
                UnionOwnershipDeleteEvent(ownershipId)
            }
        }
    }
}
