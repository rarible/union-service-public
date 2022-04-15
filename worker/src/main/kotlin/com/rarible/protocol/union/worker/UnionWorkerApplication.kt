package com.rarible.protocol.union.worker

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class UnionWorkerApplication: CommandLineRunner {
    override fun run(vararg args: String?) {

    }
}

fun main(args: Array<String>) {
    runApplication<UnionWorkerApplication>(*args)
}