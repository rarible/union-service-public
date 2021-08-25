package com.rarible.protocol.union.listener.handler.ethereum

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.ethereum.domain.Blockchain
import com.rarible.protocol.union.dto.UnionItemEventDto
import com.rarible.protocol.union.dto.UnionOrderEventDto
import com.rarible.protocol.union.dto.UnionOwnershipEventDto
import org.springframework.stereotype.Component

@Component
class EthereumEventHandlerFactory(
    private val itemEventProducer: RaribleKafkaProducer<UnionItemEventDto>,
    private val ownershipEventProducer: RaribleKafkaProducer<UnionOwnershipEventDto>,
    private val orderEventProducer: RaribleKafkaProducer<UnionOrderEventDto>
) {
    fun createItemEventHandler(blockchain: Blockchain): EthereumItemEventHandler {
        return EthereumItemEventHandler(blockchain, itemEventProducer)
    }

    fun createOwnershipEventHandler(blockchain: Blockchain): EthereumOwnershipEventHandler {
        return EthereumOwnershipEventHandler(blockchain, ownershipEventProducer)
    }

    fun createOrderEventHandler(blockchain: Blockchain): EthereumOrderEventHandler {
        return EthereumOrderEventHandler(blockchain, orderEventProducer)
    }
}
