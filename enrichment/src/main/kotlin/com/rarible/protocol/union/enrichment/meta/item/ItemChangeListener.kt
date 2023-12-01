package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.protocol.union.enrichment.model.ShortItemChange

interface ItemChangeListener {
    suspend fun onItemChange(change: ShortItemChange)
}
