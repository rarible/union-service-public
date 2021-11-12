package com.rarible.protocol.union.integration.tezos.event

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.tezos.dto.ItemEventDto
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.integration.tezos.converter.TezosItemConverter
import org.slf4j.LoggerFactory

open class TezosItemEventHandler(
    override val handler: IncomingEventHandler<UnionItemEvent>
) : AbstractBlockchainEventHandler<ItemEventDto, UnionItemEvent>(BlockchainDto.TEZOS) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @CaptureTransaction("ItemEvent#TEZOS")
    override suspend fun handle(event: ItemEventDto) {
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
