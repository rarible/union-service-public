package com.rarible.protocol.union.core

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
    val enableItemSaveImmediateToElasticSearch: Boolean = false,
    val enableItemBestBidsByCurrency: Boolean = true,
    val enableIncrementalItemStats: Boolean = true,
    // traits
    val enableTraitSaveImmediateToElasticSearch: Boolean = false,

    val enableElasticsearchCompatibilityMode: Boolean = true,

    val enableCollectionAutoReveal: Boolean = true,
    val enableCollectionSetBaseUriEvent: Boolean = true,
    val enableCollectionAutoRefreshOnCreation: Boolean = false,
    val enableCollectionItemMetaRefreshApi: Boolean = true,

    val enableOptimizedSearchForItems: Boolean = false,
    val enableOptimizedSearchForActivities: Boolean = true,
    val enableOptimizedSearchForOwnerships: Boolean = true,
    val enableOptimizedSearchForTraits: Boolean = false,

    val enableStrictMetaComparison: Boolean = false,
    val enableMetaDownloadLimit: Boolean = true,

    // migrator
    val skipOrderMigration: Boolean = false,
)
