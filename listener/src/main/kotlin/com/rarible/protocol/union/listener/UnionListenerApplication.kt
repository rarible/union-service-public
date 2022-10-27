package com.rarible.protocol.union.listener

import ch.sbb.esta.openshift.gracefullshutdown.GracefulshutdownSpringApplication
import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.protocol.union.core.handler.KafkaConsumerWorker
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class UnionListenerApplication(
    private val kafkaConsumers: List<KafkaConsumerWorker<*>>,
    private val jobs: List<SequentialDaemonWorker>
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        kafkaConsumers.forEach { it.start() }
        jobs.forEach { it.start() }
    }
}

fun main(args: Array<String>) {
    GracefulshutdownSpringApplication.run(UnionListenerApplication::class.java, *args)
}
