package com.rarible.protocol.union.search.indexer

import com.rarible.core.daemon.sequential.ConsumerWorker
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication
class UnionSearchIndexerApp(
    private val consumerWorkers: List<ConsumerWorker<*>>
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        consumerWorkers.forEach { it.start() }
    }
}

fun main(args: Array<String>) {
    runApplication<UnionSearchIndexerApp>(*args)
}
