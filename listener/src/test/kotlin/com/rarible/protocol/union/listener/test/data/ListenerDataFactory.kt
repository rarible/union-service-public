package com.rarible.protocol.union.listener.test.data

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.ReconciliationMarkType
import com.rarible.protocol.union.enrichment.model.ReconciliationMark
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.listener.config.CollectionStatisticsResyncProperties
import com.rarible.protocol.union.listener.config.InternalConsumerProperties
import com.rarible.protocol.union.listener.config.MetaItemRetry
import com.rarible.protocol.union.listener.config.MetaSchedulingProperties
import com.rarible.protocol.union.listener.config.MetricsProperties
import com.rarible.protocol.union.listener.config.PlatformBestSellCleanUpProperties
import com.rarible.protocol.union.listener.config.PriceUpdateProperties
import com.rarible.protocol.union.listener.config.ReconcileMarksProperties
import com.rarible.protocol.union.listener.config.ReconciliationProperties
import com.rarible.protocol.union.listener.config.UnionListenerProperties

fun defaultUnionListenerProperties(): UnionListenerProperties {
    return UnionListenerProperties(
        reconciliation = ReconciliationProperties(),
        priceUpdate = PriceUpdateProperties(),
        reconcileMarks = ReconcileMarksProperties(),
        consumer = InternalConsumerProperties("doesn't matter", mapOf()),
        platformBestSellCleanup = PlatformBestSellCleanUpProperties(enabled = false),
        collectionStatisticsResync = CollectionStatisticsResyncProperties(),
        metrics = MetricsProperties(),
        metaScheduling = MetaSchedulingProperties(),
        metaItemRetry = MetaItemRetry()
    )
}

fun randomItemMark(): ReconciliationMark {
    return ReconciliationMark(
        id = randomEthItemId().fullId(),
        type = ReconciliationMarkType.ITEM,
        lastUpdatedAt = nowMillis()
    )
}

fun randomCollectionMark(): ReconciliationMark {
    return ReconciliationMark(
        id = randomEthCollectionId().fullId(),
        type = ReconciliationMarkType.COLLECTION,
        lastUpdatedAt = nowMillis()
    )
}
