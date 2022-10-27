package com.rarible.protocol.union.meta.loader

import com.rarible.protocol.union.core.handler.KafkaConsumerWorker
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class UnionMetaLoaderApplication(
    private val kafkaConsumers: List<KafkaConsumerWorker<*>>
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        kafkaConsumers.forEach { it.start() }
    }
}

fun main(args: Array<String>) {
    runApplication<UnionMetaLoaderApplication>(*args)
}
