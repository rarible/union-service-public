package com.rarible.protocol.union.worker.test

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.model.ReconciliationMarkType
import com.rarible.protocol.union.enrichment.model.ReconciliationMark
import com.rarible.protocol.union.integration.ethereum.data.randomEthCollectionId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId

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
