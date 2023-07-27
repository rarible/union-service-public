package com.rarible.protocol.union.core.model.elastic

import com.rarible.protocol.union.dto.BlockchainDto

sealed class EsCollectionFilter {
    abstract val cursor: String?
}

data class EsCollectionGenericFilter(
    val blockchains: Set<BlockchainDto> = emptySet(),
    // TODO should be collection of UnionAddress
    val owners: Set<String> = emptySet(),
    override val cursor: String? = null,
) : EsCollectionFilter()

data class EsCollectionTextFilter(
    val blockchains: Set<BlockchainDto> = emptySet(),
    val text: String,
    override val cursor: String?,
) : EsCollectionFilter()
