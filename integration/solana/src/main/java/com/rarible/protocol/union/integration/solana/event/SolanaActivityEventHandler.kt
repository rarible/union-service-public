package com.rarible.protocol.union.integration.solana.event

import com.rarible.core.apm.CaptureTransaction
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
    BlockchainDto.SOLANA
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @CaptureTransaction("ActivityEvent#SOLANA")
    override suspend fun handle(event: com.rarible.protocol.solana.dto.ActivityDto) = handler.onEvent(convert(event))

    @CaptureTransaction("ActivityEvents#SOLANA")
    override suspend fun handle(events: List<com.rarible.protocol.solana.dto.ActivityDto>) = handler.onEvents(
        events.map { convert(it) }
    )

    suspend fun convert(event: com.rarible.protocol.solana.dto.ActivityDto): ActivityDto {
        logger.debug("Received Solana ({}) Activity event: type={}", event::class.java.simpleName)
        return solanaActivityConverter.convert(event, blockchain)
    }
}
