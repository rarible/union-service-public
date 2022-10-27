package com.rarible.protocol.union.search.indexer

import ch.sbb.esta.openshift.gracefullshutdown.GracefulshutdownSpringApplication
import com.rarible.core.daemon.sequential.ConsumerWorkerHolder
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class UnionSearchIndexerAppplication(
    private val consumerWorkers: List<ConsumerWorkerHolder<*>>,
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        consumerWorkers.forEach { it.start() }
    }
}

fun main(args: Array<String>) {
    GracefulshutdownSpringApplication.run(UnionSearchIndexerAppplication::class.java, *args)
}
