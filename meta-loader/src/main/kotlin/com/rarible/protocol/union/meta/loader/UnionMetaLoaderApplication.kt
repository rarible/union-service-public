package com.rarible.protocol.union.meta.loader

import com.rarible.core.kafka.RaribleKafkaConsumerWorker
import com.rarible.protocol.union.meta.loader.job.DownloadExecutorWorker
import org.springframework.boot.CommandLineRunner
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
    runApplication<UnionMetaLoaderApplication>(*args)
}
