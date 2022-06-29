package com.rarible.protocol.union.search.indexer.test

import com.rarible.core.daemon.sequential.ConsumerWorkerHolder
import org.springframework.context.annotation.Configuration
import javax.annotation.PostConstruct

@Configuration
class TestUnionSearchConfiguration(
    private val consumerWorkers: List<ConsumerWorkerHolder<*>>,
) {
    @PostConstruct
    fun postConstruct() {
        consumerWorkers.forEach { it.start() }
    }
}