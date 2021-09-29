package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.protocol.dto.NftItemDeleteEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftItemUpdateEventDto
import com.rarible.protocol.union.core.ethereum.converter.EthConverter
import com.rarible.protocol.union.core.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.service.event.ItemEventService
import com.rarible.protocol.union.listener.handler.AbstractEventHandler
import org.slf4j.LoggerFactory

class EthereumItemEventHandler(
    private val itemEventService: ItemEventService,
    private val blockchain: BlockchainDto
) : AbstractEventHandler<NftItemEventDto>() {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handleSafely(event: NftItemEventDto) {
        logger.debug("Received Ethereum ({}) Item event: type={}", blockchain, event::class.java.simpleName)

        when (event) {
            is NftItemUpdateEventDto -> {
                val item = EthItemConverter.convert(event.item, blockchain)
                itemEventService.onItemUpdated(item)
            }
            is NftItemDeleteEventDto -> {
                val itemId = ShortItemId(
                    blockchain = blockchain,
                    token = EthConverter.convert(event.item.token),
                    tokenId = event.item.tokenId
                )
                itemEventService.onItemDeleted(itemId)
            }
        }
    }
}
