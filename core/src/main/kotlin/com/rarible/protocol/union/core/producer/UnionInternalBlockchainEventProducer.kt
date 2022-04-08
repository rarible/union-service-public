package com.rarible.protocol.union.core.producer

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.model.UnionInternalBlockchainEvent
import com.rarible.protocol.union.dto.BlockchainDto

/**
 * Producer pack for internal events. Since each blockchain should be processed via separate topic,
 * this "mega-producer" contains producers for all blockchains
 */
class UnionInternalBlockchainEventProducer(
    private val producers: Map<BlockchainDto, RaribleKafkaProducer<UnionInternalBlockchainEvent>>
) {

    fun getProducer(blockchain: BlockchainDto): RaribleKafkaProducer<UnionInternalBlockchainEvent> {
        return producers[blockchain]!!
    }
}