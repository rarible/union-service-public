package com.rarible.protocol.union.worker.job.collection

interface CustomCollectionItemFetcher {

    suspend fun next(state: String?, batchSize: Int): CustomCollectionItemBatch

}