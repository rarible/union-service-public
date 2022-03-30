package com.rarible.apiservice.updatelistener

import com.rarible.core.apm.withTransaction
import com.rarible.protocol.union.api.updatelistener.AbstractUpdateListener
import com.rarible.protocol.union.dto.ItemEventDto
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

@Component
class ItemUpdateListener : AbstractUpdateListener()
   /* private val extendedItemService: ExtendedItemService*/
{
    private val sink = Sinks.many().multicast().directBestEffort<ItemEventDto>()
    val updates: Flux<ItemEventDto> = sink.asFlux()

    //@KafkaListener(topics = ["\${rarible.common.itemTopic}"], containerFactory = "itemTopicListenerContainerFactory")
    @KafkaListener(topics = ["rarible.common.itemTopic"], containerFactory = "itemTopicListenerContainerFactory")
    fun receive(@Payload changes: List<ItemEventDto>) {
        runBlocking {
            withTransaction(name = "ItemUpdateListener#receive") {
                changes.forEach { onUpdate(it) }
            }
        }
    }

    private suspend fun onUpdate(item: ItemEventDto) {
        try {
            sink.tryEmitNext(/*extendedItemService.convertItem(item)*/item) //TODO
        } catch (e: Exception) {
            logger.error("unable to send to sink", e)
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(ItemUpdateListener::class.java)
    }
}
