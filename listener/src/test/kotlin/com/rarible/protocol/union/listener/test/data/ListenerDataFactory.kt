package com.rarible.protocol.union.listener.test.data

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.enrichment.model.ReconciliationMark
import com.rarible.protocol.union.enrichment.model.ReconciliationMarkType
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
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

fun randomItemMark(): ReconciliationMark {
    return ReconciliationMark(
        id = randomEthItemId().fullId(),
        type = ReconciliationMarkType.ITEM,
        lastUpdatedAt = nowMillis()
    )
}