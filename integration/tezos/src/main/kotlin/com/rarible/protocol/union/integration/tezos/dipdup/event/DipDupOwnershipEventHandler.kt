package com.rarible.protocol.union.integration.tezos.dipdup.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.dipdup.listener.model.DipDupDeleteOwnershipEvent
import com.rarible.dipdup.listener.model.DipDupOwnershipEvent
import com.rarible.dipdup.listener.model.DipDupUpdateOwnershipEvent
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.core.model.stubEventMark
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipIdDto
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupOwnershipConverter
import org.slf4j.LoggerFactory
import java.math.BigInteger

open class DipDupOwnershipEventHandler(
    override val handler: IncomingEventHandler<UnionOwnershipEvent>,
    private val mapper: ObjectMapper
) : AbstractBlockchainEventHandler<DipDupOwnershipEvent, UnionOwnershipEvent>(
    BlockchainDto.TEZOS,
    EventType.OWNERSHIP
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(event: DipDupOwnershipEvent): UnionOwnershipEvent {
        logger.info("Received DipDup ownership event: {}", mapper.writeValueAsString(event))
        return when (event) {
            is DipDupUpdateOwnershipEvent -> {
                val ownership = DipDupOwnershipConverter.convert(event.ownership)
                UnionOwnershipUpdateEvent(ownership, stubEventMark())
            }
            is DipDupDeleteOwnershipEvent -> {
                val (contract, tokenId, owner) = event.ownershipId.split(":")
                val ownershipId = OwnershipIdDto(
                    blockchain = blockchain,
                    contract = contract,
                    tokenId = BigInteger(tokenId),
                    owner = owner
                )
                UnionOwnershipDeleteEvent(ownershipId, stubEventMark())
            }
        }
    }
}
