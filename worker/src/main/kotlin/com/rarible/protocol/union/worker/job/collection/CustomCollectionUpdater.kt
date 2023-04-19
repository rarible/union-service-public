package com.rarible.protocol.union.worker.job.collection

import com.rarible.protocol.union.core.model.UnionItem

interface CustomCollectionUpdater {

    suspend fun update(item: UnionItem)

}