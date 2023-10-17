package com.rarible.protocol.union.core

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties("common.feature-flags")
@ConstructorBinding
data class FeatureFlagsProperties(
    val enableOwnershipSourceEnrichment: Boolean = false,
    val enablePoolOrders: Boolean = true,
    val enableWebClientConnectionPool: Boolean = true,
    // activities
    val enableActivityQueriesToElasticSearch: Boolean = true,
    val enableActivityAscQueriesWithApiMerge: Boolean = true,
    val enableActivitySaveImmediateToElasticSearch: Boolean = false,
    // orders
    val enableOrderQueriesToElasticSearch: Boolean = false,
    val enableOrderSaveImmediateToElasticSearch: Boolean = false,
    // collections
    val enableSearchCollections: Boolean = true,
    val enableCollectionSaveImmediateToElasticSearch: Boolean = false,
    // ownerships
    var enableOwnershipQueriesToElasticSearch: Boolean = true,
    val enableOwnershipSaveImmediateToElasticSearch: Boolean = false,
    // items
    val enableItemQueriesToElasticSearch: Boolean = true,
    val enableSearchItems: Boolean = true,
    val enableItemSaveImmediateToElasticSearch: Boolean = false,
    val enableItemBestBidsByCurrency: Boolean = true,
    val enableIncrementalItemStats: Boolean = true,

    val enableElasticsearchCompatibilityMode: Boolean = true,

    val enableCollectionAutoReveal: Boolean = true,
    val enableCollectionSetBaseUriEvent: Boolean = true,
    val enableCollectionAutoRefreshOnCreation: Boolean = false,
    val enableCollectionItemMetaRefreshApi: Boolean = true,

    val enableOptimizedSearchForItems: Boolean = false,
    val enableOptimizedSearchForActivities: Boolean = true,
    val enableOptimizedSearchForOwnerships: Boolean = true,

    val enableStrictMetaComparison: Boolean = false,
    val enableMetaDownloadLimit: Boolean = true,
)
