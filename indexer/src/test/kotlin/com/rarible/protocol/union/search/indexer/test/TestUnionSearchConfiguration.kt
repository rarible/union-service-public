package com.rarible.protocol.union.search.indexer.test

import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import org.springframework.context.annotation.Configuration
import javax.annotation.PostConstruct

@Configuration
class TestUnionSearchConfiguration(
    private val kafkaConsumers: List<RaribleKafkaConsumerWorker<*>>,
) {
    @PostConstruct
    fun postConstruct() {
        kafkaConsumers.forEach { it.start() }
    }
}