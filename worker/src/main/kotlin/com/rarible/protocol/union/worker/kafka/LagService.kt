package com.rarible.protocol.union.worker.kafka

import kotlinx.coroutines.future.await
import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.KafkaFuture
import org.slf4j.LoggerFactory
import java.util.concurrent.CompletableFuture

class LagService(
    private val kafkaConsumer: KafkaConsumer<String, String>,
    private val metaConsumerGroup: String,
    private val bootstrapServers: String,
    private val refreshTopic: String
) {
    suspend fun isLagOk(maxLag: Long): Boolean {
        val lag =
            AdminClient.create(mapOf(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers)).use { admin ->
                val offsets = admin.listConsumerGroupOffsets(metaConsumerGroup).partitionsToOffsetAndMetadata()
                    .toCompletableFuture()
                    .await()

                val keys = offsets.keys
                    .filter {
                        it.topic() == refreshTopic
                    }
                if (keys.isEmpty()) {
                    0
                } else {
                    val endOffsets = kafkaConsumer.endOffsets(keys)
                    var offset: Long = 0
                    keys.forEach {
                        offset += (endOffsets[it] ?: 0) - (offsets[it]?.offset() ?: 0)
                    }
                    offset
                }
            }
        logger.info("Current lag of $metaConsumerGroup is $lag")
        return lag < maxLag
    }

    companion object {
        private val logger = LoggerFactory.getLogger(LagService::class.java)
    }
}

private fun <T> KafkaFuture<T>.toCompletableFuture(): CompletableFuture<T> {
    val wrappingFuture = CompletableFuture<T>()
    whenComplete { value: T, throwable: Throwable? ->
        if (throwable != null) {
            wrappingFuture.completeExceptionally(throwable)
        } else {
            wrappingFuture.complete(value)
        }
    }
    return wrappingFuture
}
