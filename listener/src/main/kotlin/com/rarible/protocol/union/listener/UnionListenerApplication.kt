package com.rarible.protocol.union.listener

import com.rarible.core.daemon.sequential.ConsumerWorker
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.OrderEventDto
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class UnionListenerApplication(
    private val ethItemChangeWorker: ConsumerWorker<NftItemEventDto>,
    private val ethOwnershipChangeWorker: ConsumerWorker<NftOwnershipEventDto>,
    private val ethOrderChangeWorker: ConsumerWorker<OrderEventDto>
) : CommandLineRunner {
    override fun run(vararg args: String?) {
        ethItemChangeWorker.start()
        ethOwnershipChangeWorker.start()
        ethOrderChangeWorker.start()
    }
}

fun main(args: Array<String>) {
    runApplication<UnionListenerApplication>(*args)
}
