package com.rarible.protocol.union.search.indexer

import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class UnionSearchIndexerApplication(
    private val kafkaConsumers: List<RaribleKafkaConsumerWorker<*>>,
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        kafkaConsumers.forEach { it.start() }
    }
}

fun main(args: Array<String>) {
    runApplication<UnionSearchIndexerApplication>(*args)
}
