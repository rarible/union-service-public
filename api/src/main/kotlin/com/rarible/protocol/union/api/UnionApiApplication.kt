package com.rarible.protocol.union.api

import ch.sbb.esta.openshift.gracefullshutdown.GracefulshutdownSpringApplication
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class UnionApiApplication(
    private val kafkaConsumers: List<RaribleKafkaConsumerWorker<*>>,
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        kafkaConsumers.forEach { it.start() }
    }
}

fun main(args: Array<String>) {
    GracefulshutdownSpringApplication.run(UnionApiApplication::class.java, *args)
}
