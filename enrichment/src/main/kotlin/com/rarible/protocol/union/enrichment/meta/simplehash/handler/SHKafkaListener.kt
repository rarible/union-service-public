package com.rarible.protocol.union.enrichment.meta.simplehash.handler

import com.simplehash.v0.nft
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.listener.BatchMessageListener
import org.springframework.stereotype.Component

@Component
class SHKafkaListener : BatchMessageListener<String, nft> {

    override fun onMessage(p0: MutableList<ConsumerRecord<String, nft>>) {
        println(p0)
        TODO("Not yet implemented")
    }

}