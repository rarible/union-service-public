package com.rarible.protocol.union.integration.solana.event

import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.solana.converter.SolanaActivityConverter
import org.slf4j.LoggerFactory

open class SolanaActivityEventHandler(
    override val handler: IncomingEventHandler<UnionActivityDto>,
    private val solanaActivityConverter: SolanaActivityConverter
) : AbstractBlockchainEventHandler<com.rarible.protocol.solana.dto.ActivityDto, UnionActivityDto>(
    BlockchainDto.SOLANA,
    EventType.ACTIVITY
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(event: com.rarible.protocol.solana.dto.ActivityDto): UnionActivityDto {
        logger.debug("Received Solana ({}) Activity event: type={}", event, event::class.java.simpleName)
        return solanaActivityConverter.convert(event, blockchain)
    }
}
