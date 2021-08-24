package com.rarible.protocol.union.listener

import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.union.listener.handler.ethereum.EthereumCompositeConsumerWorker
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class UnionListenerApplication(
    private val ethItemChangeWorker: EthereumCompositeConsumerWorker<NftItemEventDto>,
    private val ethOwnershipChangeWorker: EthereumCompositeConsumerWorker<NftOwnershipEventDto>,
    private val ethOrderChangeWorker: EthereumCompositeConsumerWorker<OrderEventDto>
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
