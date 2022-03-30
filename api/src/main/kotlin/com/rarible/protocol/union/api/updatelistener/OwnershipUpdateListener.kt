package com.rarible.protocol.union.api.updatelistener

import com.rarible.core.apm.withTransaction
import com.rarible.protocol.union.dto.OwnershipEventDto
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks

@Component
class OwnershipUpdateListener : AbstractUpdateListener() {
    private val sink = Sinks.many().multicast().directBestEffort<OwnershipEventDto>()
    val updates: Flux<OwnershipEventDto> = sink.asFlux()

 /*   @KafkaListener(
        topics = ["\${rarible.common.ownershipTopic}"],
        containerFactory = "ownershipTopicListenerContainerFactory"
    )*/
 @KafkaListener(
     topics = ["rarible.common.ownershipTopic"],
     containerFactory = "ownershipTopicListenerContainerFactory"
 )
    fun receive(@Payload changes: List<OwnershipEventDto>) {
        runBlocking {
            withTransaction(name = "OwnershipUpdateListener#receive") {
                changes.forEach { onUpdate(it) }
            }
        }
    }

    private fun onUpdate(ownership: OwnershipEventDto) {
        try {
            sink.tryEmitNext(ownership)
        } catch (e: Exception) {
            logger.error("unable to send to sink", e)
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(OwnershipUpdateListener::class.java)
    }
}
