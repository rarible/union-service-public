package com.rarible.protocol.union.enrichment.custom.collection

interface CustomCollectionItemFetcher {

    suspend fun next(state: String?, batchSize: Int): CustomCollectionItemBatch

}