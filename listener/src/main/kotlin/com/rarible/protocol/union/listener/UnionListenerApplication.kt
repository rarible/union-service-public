package com.rarible.protocol.union.listener

import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import com.rarible.protocol.union.core.handler.KafkaConsumerWorker
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class UnionListenerApplication(
    private val kafkaConsumers: List<RaribleKafkaConsumerWorker<*>>,
    private val legacyKafkaConsumers: List<KafkaConsumerWorker<*>>,
    private val jobs: List<SequentialDaemonWorker>
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        kafkaConsumers.forEach { it.start() }
        legacyKafkaConsumers.forEach { it.start() }
        jobs.forEach { it.start() }
    }
}

fun main(args: Array<String>) {
    runApplication<UnionListenerApplication>(*args)
}
