package com.rarible.protocol.union.listener.test.data

import com.rarible.protocol.union.listener.config.InternalConsumerProperties
import com.rarible.protocol.union.listener.config.PriceUpdateProperties
import com.rarible.protocol.union.listener.config.ReconciliationProperties
import com.rarible.protocol.union.listener.config.UnionListenerProperties

fun defaultUnionListenerProperties(): UnionListenerProperties {
    return UnionListenerProperties(
        reconciliation = ReconciliationProperties(),
        priceUpdate = PriceUpdateProperties(),
        consumer = InternalConsumerProperties("doesn't matter", mapOf())
    )
}
