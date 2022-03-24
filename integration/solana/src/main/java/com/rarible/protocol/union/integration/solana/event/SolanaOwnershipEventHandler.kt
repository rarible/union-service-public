package com.rarible.protocol.union.integration.solana.event

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.solana.converter.SolanaOwnershipConverter
import com.rarible.solana.protocol.dto.BalanceEventDto
import com.rarible.solana.protocol.dto.BalanceUpdateEventDto
import org.slf4j.LoggerFactory

open class SolanaOwnershipEventHandler(
    override val handler: IncomingEventHandler<UnionOwnershipEvent>
) : AbstractBlockchainEventHandler<BalanceEventDto, UnionOwnershipEvent>(BlockchainDto.SOLANA) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @CaptureTransaction("OwnershipEvent#SOLANA")
    override suspend fun handle(event: BalanceEventDto) {
        logger.info("Received {} Ownership event: {}", blockchain, event)

        when (event) {
            is BalanceUpdateEventDto -> {
                val unionOwnership = SolanaOwnershipConverter.convert(event.balance, blockchain)
                handler.onEvent(UnionOwnershipUpdateEvent(unionOwnership))
            }
            // TODO what about delete event?
        }
    }

}
