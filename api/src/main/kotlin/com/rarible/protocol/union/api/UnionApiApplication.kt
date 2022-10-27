package com.rarible.protocol.union.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
class UnionApiApplication

fun main(args: Array<String>) {
    runApplication<UnionApiApplication>(*args)
}
