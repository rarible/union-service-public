package com.rarible.protocol.union.listener.test.data

import com.rarible.core.common.nowMillis
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.model.ItemReconciliationMark
import com.rarible.protocol.union.enrichment.model.OwnershipReconciliationMark
import com.rarible.protocol.union.listener.config.InternalConsumerProperties
import com.rarible.protocol.union.listener.config.OpenSeaCleanUpProperties
import com.rarible.protocol.union.listener.config.PriceUpdateProperties
import com.rarible.protocol.union.listener.config.ReconciliationProperties
import com.rarible.protocol.union.listener.config.UnionListenerProperties
import java.time.temporal.ChronoUnit

fun defaultUnionListenerProperties(): UnionListenerProperties {
    return UnionListenerProperties(
        reconciliation = ReconciliationProperties(),
        priceUpdate = PriceUpdateProperties(),
        consumer = InternalConsumerProperties("doesn't matter", mapOf()),
        openSeaCleanup = OpenSeaCleanUpProperties(
            sellOrderFrom = nowMillis().minus(10, ChronoUnit.DAYS)
        )
    )
}

fun randomItemMark(): ItemReconciliationMark {
    return ItemReconciliationMark(
        blockchain = BlockchainDto.ETHEREUM,
        token = randomString(),
        tokenId = randomBigInt(),
        lastUpdatedAt = nowMillis()
    )
}

fun randomOwnershipMark(): OwnershipReconciliationMark {
    return OwnershipReconciliationMark(
        blockchain = BlockchainDto.ETHEREUM,
        token = randomString(),
        tokenId = randomBigInt(),
        owner = randomString(),
        lastUpdatedAt = nowMillis()
    )
}
