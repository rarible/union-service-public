package com.rarible.protocol.union.enrichment.meta.item

import com.rarible.protocol.union.enrichment.model.ItemChangeEvent

interface ItemChangeListener {
    suspend fun onItemChange(change: ItemChangeEvent)
}
