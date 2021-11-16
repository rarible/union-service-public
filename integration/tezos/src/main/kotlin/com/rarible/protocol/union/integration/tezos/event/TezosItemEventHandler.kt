package com.rarible.protocol.union.integration.tezos.event

import com.rarible.core.apm.CaptureTransaction
import com.rarible.protocol.tezos.dto.TezosItemSafeEventDto
import com.rarible.protocol.union.core.handler.AbstractBlockchainEventHandler
import com.rarible.protocol.union.core.handler.IncomingEventHandler
import com.rarible.protocol.union.core.model.UnionItemDeleteEvent
import com.rarible.protocol.union.core.model.UnionItemEvent
import com.rarible.protocol.union.core.model.UnionItemUpdateEvent
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.integration.tezos.converter.TezosItemConverter
import org.slf4j.LoggerFactory

open class TezosItemEventHandler(
    override val handler: IncomingEventHandler<UnionItemEvent>
) : AbstractBlockchainEventHandler<TezosItemSafeEventDto, UnionItemEvent>(BlockchainDto.TEZOS) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @CaptureTransaction("ItemEvent#TEZOS")
    override suspend fun handle(event: TezosItemSafeEventDto) {
        logger.info("Received Tezos Item event: type={}", event::class.java.simpleName)

        when (event.type) {
            TezosItemSafeEventDto.Type.UPDATE -> {
                val item = TezosItemConverter.convert(event.item!!, blockchain)
                handler.onEvent(UnionItemUpdateEvent(item))
            }
            TezosItemSafeEventDto.Type.DELETE -> {
                val itemId = ItemIdDto(
                    blockchain = blockchain,
                    contract = event.item!!.contract,
                    tokenId = event.item!!.tokenId
                )
                handler.onEvent(UnionItemDeleteEvent(itemId))
            }
            TezosItemSafeEventDto.Type.SERIALIZATION_FAILED -> {
                // skip it, will be logged inside of parser
            }
        }
    }
}
