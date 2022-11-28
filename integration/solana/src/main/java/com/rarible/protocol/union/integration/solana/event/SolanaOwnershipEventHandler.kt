package com.rarible.protocol.union.integration.solana.event

import com.rarible.protocol.solana.dto.BalanceDeleteEventDto
import com.rarible.protocol.solana.dto.BalanceEventDto
import com.rarible.protocol.solana.dto.BalanceUpdateEventDto
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionOwnershipDeleteEvent
import com.rarible.protocol.union.core.model.UnionOwnershipEvent
import com.rarible.protocol.union.core.model.UnionOwnershipUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.solana.converter.SolanaOwnershipConverter
import org.slf4j.LoggerFactory

open class SolanaOwnershipEventHandler(
    override val handler: IncomingEventHandler<UnionOwnershipEvent>
) : AbstractBlockchainEventHandler<BalanceEventDto, UnionOwnershipEvent>(
    BlockchainDto.SOLANA,
    EventType.OWNERSHIP
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(event: BalanceEventDto): UnionOwnershipEvent? {
        logger.info("Received {} Ownership event: {}", blockchain, event)

        val isAssociatedTokenAccount = when (event) {
            is BalanceUpdateEventDto -> event.balance.isAssociatedTokenAccount == null || event.balance.isAssociatedTokenAccount!!
            is BalanceDeleteEventDto -> event.balance.isAssociatedTokenAccount == null || event.balance.isAssociatedTokenAccount!!
        }

        if (!isAssociatedTokenAccount) {
            logger.info("Skipping balance event for the secondary account ${event.account} of ${event.mint}")
            return null
        }

        return when (event) {
            is BalanceUpdateEventDto -> {
                val unionOwnership = SolanaOwnershipConverter.convert(event.balance, blockchain)
                UnionOwnershipUpdateEvent(unionOwnership)
            }
            is BalanceDeleteEventDto -> {
                val unionOwnership = SolanaOwnershipConverter.convert(event.balance, blockchain)
                UnionOwnershipDeleteEvent(unionOwnership.id)
            }
        }
    }
}
