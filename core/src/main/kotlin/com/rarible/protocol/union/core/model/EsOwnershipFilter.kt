package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders.boolQuery
import org.elasticsearch.index.query.QueryBuilders.idsQuery
import org.elasticsearch.index.query.QueryBuilders.rangeQuery
import org.elasticsearch.index.query.QueryBuilders.termQuery
import org.elasticsearch.index.query.QueryBuilders.termsQuery
import org.elasticsearch.search.sort.SortBuilders.fieldSort
import org.elasticsearch.search.sort.SortOrder
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.data.elasticsearch.core.query.Query

sealed interface EsOwnershipFilter {
    val cursor: String?
}

data class EsOwnershipByOwnerFilter(
    val owner: UnionAddress,
    val blockchains: Collection<BlockchainDto>? = null,
    override val cursor: String? = null,
) : EsOwnershipFilter

data class EsOwnershipByItemFilter(
    val itemId: ItemIdDto,
    override val cursor: String? = null,
) : EsOwnershipFilter
