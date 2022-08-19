package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.BlockchainDto
import java.time.Instant

sealed class EsItemFilter {
    abstract val blockchains: Set<String>?
    abstract val cursor: String?
}

data class EsItemGenericFilter(
    override val blockchains: Set<String>? = null,
    val itemIds: Set<String>? = null,
    val creators: Set<String> = emptySet(),
    val owners: Set<String> ? = null,
    val collections: Set<String>? = null,
    val mintedFrom: Instant? = null,
    val mintedTo: Instant? = null,
    val updatedFrom: Instant? = null,
    val updatedTo: Instant? = null,
    val deleted: Boolean? = null,
    val text: String? = null,
    val traitsKeys: Set<String>? = null,
    val traitsValues: Set<String>? = null,
    val descriptions: Set<String>? = null,
    val sellPlatforms: Set<String>? = null,
    val bidPlatforms: Set<String>? = null,
    val sellPriceCurrency: String? = null,
    val sellPriceFrom: Double? = null,
    val sellPriceTo: Double? = null,
    val bidPriceCurrency: String? = null,
    val bidPriceFrom: Double? = null,
    val bidPriceTo: Double? = null,
    override val cursor: String? = null,
) : EsItemFilter()
