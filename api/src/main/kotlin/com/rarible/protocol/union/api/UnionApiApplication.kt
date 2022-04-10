package com.rarible.protocol.union.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.reactive.config.EnableWebFlux

@SpringBootApplication
class UnionApiApplication

fun main(args: Array<String>) {
    runApplication<UnionApiApplication>(*args)
}
