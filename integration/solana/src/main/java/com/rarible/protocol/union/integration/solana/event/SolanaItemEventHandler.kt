package com.rarible.protocol.union.integration.solana.event

import com.rarible.protocol.solana.dto.TokenDeleteEventDto
import com.rarible.protocol.solana.dto.TokenEventDto
import com.rarible.protocol.solana.dto.TokenUpdateEventDto
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.core.model.stubEventMark
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.integration.solana.converter.SolanaItemConverter
import org.slf4j.LoggerFactory

open class SolanaItemEventHandler(
    override val handler: IncomingEventHandler<UnionItemEvent>
) : AbstractBlockchainEventHandler<TokenEventDto, UnionItemEvent>(
    BlockchainDto.SOLANA,
    EventType.ITEM
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(event: TokenEventDto): UnionItemEvent {
        logger.info("Received {} Item event: {}", blockchain, event)

        return when (event) {
            is TokenUpdateEventDto -> {
                val item = SolanaItemConverter.convert(event.token, blockchain)
                UnionItemUpdateEvent(item, stubEventMark())
            }
            is TokenDeleteEventDto -> {
                val itemId = ItemIdDto(
                    blockchain = blockchain,
                    value = event.address
                )
                UnionItemDeleteEvent(itemId, stubEventMark())
            }
        }
    }
}
