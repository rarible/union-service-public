package com.rarible.protocol.union.api.updatelistener

import org.apache.kafka.common.TopicPartition
import org.springframework.kafka.listener.ConsumerSeekAware

/**
 * Базовый класс для всех листенеров. Устанавливает offset на последнее значение при старте.
 */
abstract class AbstractUpdateListener : ConsumerSeekAware {
    override fun onPartitionsAssigned(
        assignments: Map<TopicPartition, Long?>,
        callback: ConsumerSeekAware.ConsumerSeekCallback
    ) {
        assignments.forEach { (t: TopicPartition, _: Long?) ->
            callback.seekToEnd(
                t.topic(),
                t.partition()
            )
        }
    }
}
