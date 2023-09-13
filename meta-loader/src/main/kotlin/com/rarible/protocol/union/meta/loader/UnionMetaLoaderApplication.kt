package com.rarible.protocol.union.meta.loader

import com.rarible.core.kafka.KafkaShutdownHook
import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import com.rarible.protocol.union.meta.loader.job.DownloadExecutorWorker
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class UnionMetaLoaderApplication(
    private val kafkaConsumers: List<RaribleKafkaConsumerWorker<*>>,
    private val jobs: List<DownloadExecutorWorker>
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        kafkaConsumers.forEach { it.start() }
        jobs.forEach { it.start() }
    }
}

fun main(args: Array<String>) {
    val app = SpringApplication(UnionMetaLoaderApplication::class.java)
    app.setRegisterShutdownHook(false)
    val context = app.run(*args)
    Runtime.getRuntime().addShutdownHook(Thread(KafkaShutdownHook(context, context::close)))
}
