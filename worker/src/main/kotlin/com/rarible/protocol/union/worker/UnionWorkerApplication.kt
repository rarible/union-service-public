package com.rarible.protocol.union.worker

import com.rarible.core.daemon.sequential.SequentialDaemonWorker
import com.rarible.core.kafka.KafkaShutdownHook
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class UnionWorkerApplication(
    private val jobs: List<SequentialDaemonWorker>
) : CommandLineRunner {

    override fun run(vararg args: String?) {
        jobs.forEach { it.start() }
    }
}

fun main(args: Array<String>) {
    val app = SpringApplication(UnionWorkerApplication::class.java)
    app.setRegisterShutdownHook(false)
    val context = app.run(*args)
    Runtime.getRuntime().addShutdownHook(Thread(KafkaShutdownHook(context, context::close)))
}
