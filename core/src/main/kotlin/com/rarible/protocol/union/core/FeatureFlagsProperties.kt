package com.rarible.protocol.union.core

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding

@ConfigurationProperties("common.feature-flags")
@ConstructorBinding
data class FeatureFlagsProperties(
    val enableNotificationOnCollectionOrders: Boolean = true,
    val enableRevertedActivityEventSending: Boolean = false,
    val enableOwnershipSourceEnrichment: Boolean = false,
    val enableItemLastSaleEnrichment: Boolean = true,
    val enableLegacyWrappedEventTopic: Boolean = true,
    // activities
    var enableActivityQueriesToElasticSearch: Boolean = false,
    var enableActivityQueriesPerTypeFilter: Boolean = false,
    var enableImmutableXActivitiesQueries: Boolean = false,
    // orders
    var enableOrderQueriesToElasticSearch: Boolean = false,
)