package com.rarible.service.activity

import com.rarible.core.apm.withTransaction
import com.rarible.core.daemon.sequential.ConsumerEventHandler
import com.rarible.coroutine.asyncWithTraceId
import com.rarible.logging.withBatchId
import com.rarible.logging.withTraceId
import com.rarible.protocol.union.dto.ActivityDto
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.messaging.handler.annotation.Payload

open class BatchActivityEventHandler(private val eventHandler: ConsumerEventHandler<ActivityDto>) {

    @KafkaListener(
        topics = ["\${rarible.common.kafka.union.activitiesTopic}"],
        containerFactory = "unionActivityListenerContainerFactory"
    )
    fun receive(
        @Payload events: List<ActivityDto>
    ) {
        runBlocking {
            withTransaction(name = "BatchActivityEventHandler#receive") {
                withBatchId {
                    logger.info("Processing ${events.size} union activities")
                    val uniqueEvents = events.groupBy { it.id }.values

                    uniqueEvents.map { event ->
                        asyncWithTraceId {
                            withTraceId {
                                val activityEvent = event.last()
                                for (i in 1..3) {
                                    try {
                                        eventHandler.handle(activityEvent)
                                        return@withTraceId
                                    } catch (e: Exception) {
                                        logger.error(
                                            "Failed to handle union activity ${activityEvent.id}. Attempt $i",
                                            e
                                        )
                                    }
                                }
                            }
                        }
                    }.awaitAll()
                }
            }
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(BatchActivityEventHandler::class.java)
    }
}
