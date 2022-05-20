package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.BlockchainDto
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.RangeQueryBuilder
import org.elasticsearch.index.query.TermQueryBuilder
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder

sealed class EsCollectionFilter {
    abstract val cursor: String?
}

data class EsCollectionGenericFilter(
    val blockchains: Set<BlockchainDto> = emptySet(),
    val owners: Set<String> = emptySet(),
    override val cursor: String? = null,
) : EsCollectionFilter()
