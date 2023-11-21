package com.rarible.protocol.union.core.model.elastic

import java.time.Instant

sealed class EsItemFilter {
    abstract val blockchains: Set<String>?
    abstract val cursor: String?
}

data class EsItemGenericFilter(
    override val blockchains: Set<String>? = null,
    val itemIds: Set<String>? = null,
    val creators: Set<String> = emptySet(),
    val owners: Set<String>? = null,
    val collections: Set<String>? = null,
    val names: Set<String>? = null,
    val mintedFrom: Instant? = null,
    val mintedTo: Instant? = null,
    val updatedFrom: Instant? = null,
    val updatedTo: Instant? = null,
    val deleted: Boolean? = null,
    val text: String? = null,
    val fullText: FullTextSearch? = null,
    val traits: List<TraitFilter>? = null,
    val traitRanges: List<TraitRangeFilter>? = null,
    val sellPlatforms: Set<String>? = null,
    val bidPlatforms: Set<String>? = null,
    val sellPriceCurrency: String? = null,
    val sellPriceFrom: Double? = null,
    val sellPriceTo: Double? = null,
    val bidPriceCurrency: String? = null,
    val bidPriceFrom: Double? = null,
    val bidPriceTo: Double? = null,
    val onSale: Boolean? = null,
    override val cursor: String? = null,
) : EsItemFilter(), DateRangeFilter<EsItemGenericFilter> {

    override val from = updatedFrom
    override val to = updatedTo

    override fun applyDateRange(range: DateRange): EsItemGenericFilter =
        copy(updatedFrom = range.from, updatedTo = range.to)
}

data class TraitFilter(val key: String, val value: String)
data class TraitRangeFilter(val key: String, val valueRange: Range)
data class Range(val from: Float?, val to: Float?)
