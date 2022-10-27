package com.rarible.protocol.union.worker

import ch.sbb.esta.openshift.gracefullshutdown.GracefulshutdownSpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class UnionWorkerApplication

fun main(args: Array<String>) {
    GracefulshutdownSpringApplication.run(UnionWorkerApplication::class.java, *args)
}