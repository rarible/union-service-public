package com.rarible.protocol.union.search.indexer

import com.rarible.core.daemon.sequential.ConsumerWorkerHolder
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class UnionSearchIndexerAppplication(
    private val consumerWorkers: List<ConsumerWorkerHolder<*>>
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        consumerWorkers.forEach { it.start() }
    }
}

fun main(args: Array<String>) {
    runApplication<UnionSearchIndexerAppplication>(*args)
}
