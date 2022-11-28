package com.rarible.protocol.union.integration.solana.event

import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.solana.converter.SolanaActivityConverter
import org.slf4j.LoggerFactory

open class SolanaActivityEventHandler(
    override val handler: IncomingEventHandler<ActivityDto>,
    private val solanaActivityConverter: SolanaActivityConverter
) : AbstractBlockchainEventHandler<com.rarible.protocol.solana.dto.ActivityDto, ActivityDto>(
    BlockchainDto.SOLANA,
    EventType.ACTIVITY
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(event: com.rarible.protocol.solana.dto.ActivityDto): ActivityDto {
        logger.debug("Received Solana ({}) Activity event: type={}", event::class.java.simpleName)
        return solanaActivityConverter.convert(event, blockchain)
    }
}
