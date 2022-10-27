package com.rarible.protocol.union.api

import ch.sbb.esta.openshift.gracefullshutdown.GracefulshutdownSpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class UnionApiApplication

fun main(args: Array<String>) {
    GracefulshutdownSpringApplication.run(UnionApiApplication::class.java, *args)
}
