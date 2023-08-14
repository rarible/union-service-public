package com.rarible.protocol.union.enrichment.meta.simplehash

import com.rarible.core.kafka.RaribleKafkaBatchEventHandler
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaService
import com.simplehash.v0.nft
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SimpleHashKafkaHandler(
    val itemMetaService: ItemMetaService,
    val simpleHashMetrics: SimpleHashItemMetrics
) : RaribleKafkaBatchEventHandler<nft> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: List<nft>) {
        event.forEach {
            val blockchain = getBlockchain(it)
            if (blockchain != null) {
                try {
                    logger.info("Received update for item ${it.nftId}")
                    val item = SimpleHashConverter.convert(it)
                    simpleHashMetrics.onEventIncomingSuccess(blockchain)
                    itemMetaService.scheduleAndSaveSimpleHashItemUpdate(item, false)
                } catch (e: Exception) {
                    logger.error("Error handling for item ${it.nftId}. It will be ignored", e)
                    simpleHashMetrics.onEventIncomingFailed(blockchain)
                }
            } else {
                simpleHashMetrics.onEventIncomingFailed()
            }
        }
    }

    fun getBlockchain(item: nft): BlockchainDto? {
        return try {
            val itemIdDto = SimpleHashConverter.parseNftId(item.nftId.toString())
            itemIdDto.blockchain
        } catch (e: Exception) {
            logger.error("Failed to parse blockchain from ${item.nftId}", e)
            null
        }
    }
}
