package com.rarible.protocol.union.enrichment.meta.simplehash

import com.rarible.core.kafka.RaribleKafkaBatchEventHandler
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaService
import com.simplehash.v0.nft
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SimpleHashKafkaHandler(
    val itemMetaService: ItemMetaService
) : RaribleKafkaBatchEventHandler<nft> {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun handle(event: List<nft>) {
        event.forEach {
            try {
                logger.info("Received update for item ${it.nftId}")
                val item = SimpleHashConverter.convert(it)
                itemMetaService.scheduleSimpleHashItemUpdate(item)
            } catch (e: Exception) {
                logger.error("Error handling for item ${it.nftId}. It will be ignored", e)
            }
        }
    }
}
