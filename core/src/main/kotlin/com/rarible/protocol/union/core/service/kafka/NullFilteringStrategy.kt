package com.rarible.service.kafka

import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.listener.adapter.RecordFilterStrategy

class NullFilteringStrategy<V> : RecordFilterStrategy<String, V> {
    override fun filter(consumerRecord: ConsumerRecord<String, V?>): Boolean = consumerRecord.value() == null
}
