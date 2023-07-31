package com.rarible.protocol.union.worker

import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class UnionWorkerApplication(
    private val jobs: List<SequentialDaemonWorker>
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        jobs.forEach { it.start() }
    }
}

fun main(args: Array<String>) {
    runApplication<UnionWorkerApplication>(*args)
}
