package com.rarible.apiservice.updatelistener

import com.rarible.apiservice.properties.ExtendedItemService
import com.rarible.core.apm.withTransaction
import com.rarible.domain.Item
import com.rarible.marketplace.generated.dto.ItemDto
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

@Component
class ItemUpdateListener(
    private val extendedItemService: ExtendedItemService
) : AbstractUpdateListener() {
    private val sink = Sinks.many().multicast().directBestEffort<ItemDto>()
    val updates: Flux<ItemDto> = sink.asFlux()

    @KafkaListener(topics = ["\${rarible.common.itemTopic}"], containerFactory = "itemTopicListenerContainerFactory")
    fun receive(@Payload changes: List<Item>) {
        runBlocking {
            withTransaction(name = "ItemUpdateListener#receive") {
                changes.forEach { onUpdate(it) }
            }
        }
    }

    private suspend fun onUpdate(item: Item) {
        try {
            sink.tryEmitNext(extendedItemService.convertItem(item))
        } catch (e: Exception) {
            logger.error("unable to send to sink", e)
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ItemUpdateListener::class.java)
    }
}
