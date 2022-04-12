package com.rarible.protocol.union.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.ComponentScan

@SpringBootApplication
@ComponentScan(basePackages = ["com.rarible.protocol.union.search.core"])
class UnionApiApplication

fun main(args: Array<String>) {
    runApplication<UnionApiApplication>(*args)
}
