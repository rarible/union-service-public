package com.rarible.protocol.union.integration.tezos.event

import com.rarible.protocol.tezos.dto.ItemEventDto
import com.rarible.protocol.union.core.handler.BlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.integration.tezos.TezosComponent
import com.rarible.protocol.union.integration.tezos.converter.TezosItemConverter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
@TezosComponent
class TezosItemEventHandler(
    override val handler: IncomingEventHandler<UnionItemEvent>
) : BlockchainEventHandler<ItemEventDto, UnionItemEvent>(BlockchainDto.TEZOS) {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: ItemEventDto) {
        logger.info("Received Tezos Item event: type={}", event::class.java.simpleName)

        when (event.type) {
            ItemEventDto.Type.UPDATE -> {
                val item = TezosItemConverter.convert(event.item!!, blockchain)
                handler.onEvent(UnionItemUpdateEvent(item))
            }
            ItemEventDto.Type.DELETE -> {
                val itemId = ItemIdDto(
                    blockchain = blockchain,
                    token = UnionAddress(blockchain, event.item!!.contract),
                    tokenId = event.item!!.tokenId
                )
                handler.onEvent(UnionItemDeleteEvent(itemId))
            }
            ItemEventDto.Type.SERIALIZATION_FAILED -> {
                // skip it, will be logged inside of parser
            }
        }
    }
}
