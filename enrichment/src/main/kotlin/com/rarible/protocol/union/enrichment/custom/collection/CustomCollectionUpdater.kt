package com.rarible.protocol.union.enrichment.custom.collection

import com.rarible.protocol.union.core.model.UnionItem

interface CustomCollectionUpdater {

    suspend fun update(item: UnionItem)

}