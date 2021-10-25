package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.protocol.dto.NftItemDeleteEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthItemConverter
import org.slf4j.LoggerFactory

class EthItemEventHandler(
    blockchain: BlockchainDto,
    override val handler: IncomingEventHandler<UnionItemEvent>
) : BlockchainEventHandler<NftItemEventDto, UnionItemEvent>(blockchain) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: NftItemEventDto) {
        logger.debug("Received Ethereum ({}) Item event: type={}", blockchain, event::class.java.simpleName)

        when (event) {
            is NftItemUpdateEventDto -> {
                val item = EthItemConverter.convert(event.item, blockchain)
                handler.onEvent(UnionItemUpdateEvent(item))
            }
            is NftItemDeleteEventDto -> {
                val itemId = ItemIdDto(
                    blockchain = blockchain,
                    token = UnionAddress(blockchain, EthConverter.convert(event.item.token)),
                    tokenId = event.item.tokenId
                )
                handler.onEvent(UnionItemDeleteEvent(itemId))
            }
        }
    }
}