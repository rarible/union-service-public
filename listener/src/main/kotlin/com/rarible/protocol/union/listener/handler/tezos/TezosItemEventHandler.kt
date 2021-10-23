package com.rarible.protocol.union.listener.handler.tezos

import com.rarible.protocol.tezos.dto.ItemEventDto
import com.rarible.protocol.union.core.tezos.converter.TezosItemConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.listener.handler.BlockchainEventHandler
import com.rarible.protocol.union.listener.service.EnrichmentItemEventService
import org.slf4j.LoggerFactory

class TezosItemEventHandler(
    override val blockchain: BlockchainDto,
    private val itemEventService: EnrichmentItemEventService
) : BlockchainEventHandler<ItemEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: ItemEventDto) {
        logger.info("Received Tezos Item event: type={}", event::class.java.simpleName)

        when (event.type) {
            ItemEventDto.Type.UPDATE -> {
                val item = TezosItemConverter.convert(event.item!!, blockchain)
                itemEventService.onItemUpdated(item)
            }
            ItemEventDto.Type.DELETE -> {
                val itemId = ItemIdDto(
                    blockchain = blockchain,
                    token = UnionAddress(blockchain, event.item!!.contract),
                    tokenId = event.item!!.tokenId
                )
                itemEventService.onItemDeleted(itemId)
            }
            ItemEventDto.Type.SERIALIZATION_FAILED -> {
                // skip it, will be logged inside of parser
            }
        }
    }
}
