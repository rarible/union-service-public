package com.rarible.protocol.union.enrichment.custom.collection.fetcher

interface CustomCollectionItemFetcher {

    suspend fun next(state: String?, batchSize: Int): CustomCollectionItemBatch

}