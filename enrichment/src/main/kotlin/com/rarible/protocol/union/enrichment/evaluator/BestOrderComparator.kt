package com.rarible.protocol.union.enrichment.evaluator

import com.rarible.protocol.union.enrichment.model.ShortOrder

interface BestOrderComparator {

    val name: String

    fun compare(current: ShortOrder, updated: ShortOrder): ShortOrder
}
