package com.rarible.protocol.union.listener.test.data

import com.rarible.protocol.union.listener.config.BlockchainConsumerSet
import com.rarible.protocol.union.listener.config.ConsumerProperties
import com.rarible.protocol.union.listener.config.ReconciliationProperties
import com.rarible.protocol.union.listener.config.UnionListenerProperties

fun defaultUnionListenerProperties(): UnionListenerProperties {
    return UnionListenerProperties(
        consumer = BlockchainConsumerSet(
            ethereum = ConsumerProperties(brokerReplicaSet = "KAFKA"),
            polygon = ConsumerProperties(brokerReplicaSet = "KAFKA"),
            flow = ConsumerProperties(brokerReplicaSet = "KAFKA")
        ),
        reconciliation = ReconciliationProperties()
    )
}