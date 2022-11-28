package com.rarible.protocol.union.integration.solana.event

import com.rarible.protocol.solana.dto.TokenMetaEventDto
import com.rarible.protocol.solana.dto.TokenMetaTriggerEventDto
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemMetaEvent
import com.rarible.protocol.union.core.model.UnionItemMetaRefreshEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import org.slf4j.LoggerFactory

open class SolanaItemMetaEventHandler(
    override val handler: IncomingEventHandler<UnionItemMetaEvent>
) : AbstractBlockchainEventHandler<TokenMetaEventDto, UnionItemMetaEvent>(
    BlockchainDto.SOLANA,
    EventType.ITEM_META
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(event: TokenMetaEventDto): UnionItemMetaEvent? {
        logger.info("Received {} token meta event: {}", blockchain, event)
        return when (event) {
            is TokenMetaTriggerEventDto -> {
                val itemId = ItemIdDto(blockchain, event.tokenAddress)
                UnionItemMetaRefreshEvent(itemId)
            }
        }
    }
}
