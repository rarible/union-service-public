package com.rarible.protocol.union.worker.job

import com.rarible.protocol.union.enrichment.custom.collection.CustomCollectionMigrator
import com.rarible.protocol.union.enrichment.custom.collection.fetcher.CustomCollectionItemFetcherFactory
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@Component
class CustomCollectionJob(
    private val customCollectionItemFetcherProvider: CustomCollectionItemFetcherFactory,
    private val customCollectionMigrator: CustomCollectionMigrator
) {

    private val batchSize = 50

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * @param name identifier of rule set in the configuration
     * @param continuation last state of the migration (format depends on the rule type)
     */
    suspend fun migrate(name: String, continuation: String?): String? {
        val fetchers = customCollectionItemFetcherProvider.get(name)
        val state = continuation?.let { State(it) }
        val currentFetcher = AtomicInteger(state?.rule ?: 0)
        val currentState = AtomicReference(state?.state)
        while (currentFetcher.get() < fetchers.size) {
            val fetcher = fetchers[currentFetcher.get()]
            val next = fetcher.next(currentState.get(), batchSize)
            if (next.state != null) {
                logger.info("Moving {} Items to custom collection: {}", next.items.size, name)
                customCollectionMigrator.migrate(next.items)
                return State(currentFetcher.get(), next.state!!).toString()
            }
            currentFetcher.incrementAndGet()
            currentState.set(null)
        }
        return null
    }

    private data class State(
        val rule: Int,
        val state: String
    ) {

        constructor(state: String) : this(
            state.substringBefore("_").toInt(),
            state.substringAfter("_")
        )

        override fun toString(): String {
            return "${rule}_$state"
        }
    }
}
