package com.rarible.protocol.union.integration.ethereum.event

import com.rarible.protocol.dto.NftItemDeleteEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.union.core.event.EventType
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthItemConverter
import org.slf4j.LoggerFactory

abstract class EthItemEventHandler(
    blockchain: BlockchainDto,
    override val handler: IncomingEventHandler<UnionItemEvent>
) : AbstractBlockchainEventHandler<NftItemEventDto, UnionItemEvent>(
    blockchain,
    EventType.ITEM
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun convert(event: NftItemEventDto): UnionItemEvent {
        logger.info("Received {} Item event {}", blockchain, event)

        return when (event) {
            is NftItemUpdateEventDto -> {
                val item = EthItemConverter.convert(event.item, blockchain)
                UnionItemUpdateEvent(item)
            }
            is NftItemDeleteEventDto -> {
                val itemId = ItemIdDto(
                    blockchain = blockchain,
                    contract = EthConverter.convert(event.item.token),
                    tokenId = event.item.tokenId
                )
                UnionItemDeleteEvent(itemId)
            }
        }
    }
}

open class EthereumItemEventHandler(
    handler: IncomingEventHandler<UnionItemEvent>
) : EthItemEventHandler(BlockchainDto.ETHEREUM, handler)

open class PolygonItemEventHandler(
    handler: IncomingEventHandler<UnionItemEvent>
) : EthItemEventHandler(BlockchainDto.POLYGON, handler)