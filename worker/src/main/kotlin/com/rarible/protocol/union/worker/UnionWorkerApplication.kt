package com.rarible.protocol.union.worker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class UnionWorkerApplication

fun main(args: Array<String>) {
    runApplication<UnionWorkerApplication>(*args)
}