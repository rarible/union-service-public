package com.rarible.protocol.union.listener

import com.rarible.protocol.union.listener.handler.KafkaConsumerWorker
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class UnionListenerApplication(
    private val kafkaConsumers: List<KafkaConsumerWorker>
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        kafkaConsumers.forEach { it.start() }
        Thread.sleep(500) //TODO producers triggers earlier than consumers started to listen topics
    }
}

fun main(args: Array<String>) {
    runApplication<UnionListenerApplication>(*args)
}
