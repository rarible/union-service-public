package com.rarible.protocol.union.worker.job.collection

import org.springframework.stereotype.Component

@Component
class CustomCollectionItemFetcherProvider {

    // TODO make it based on configuration
    fun get(name: String): List<CustomCollectionItemFetcher> {
        return emptyList()
    }

}