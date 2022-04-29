package com.rarible.protocol.union.core.model

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
    fun asQuery(): Query

    fun genericBuild(
        queryBuilder: QueryBuilder,
        continuation: DateIdContinuation?,
        size: Int,
    ): Query {
        val continuationQuery = continuation?.let {
            boolQuery()
                .should(
                    boolQuery()
                        .must(termQuery(EsOwnership::date.name, it.date))
                        .must(rangeQuery(EsOwnership::ownershipId.name).lt(it.id))
                )
                .should(
                    rangeQuery(EsOwnership::date.name).lt(it.date)
                )
        }
        val builders = listOfNotNull(queryBuilder, continuationQuery)
        val fullQueryBuilder = builders.singleOrNull()
            ?: boolQuery().also { builders.forEach(it::must) }

        return NativeSearchQueryBuilder()
            .withQuery(fullQueryBuilder)
            .withSort(fieldSort(EsOwnership::date.name).order(SortOrder.DESC))
            .withSort(fieldSort(EsOwnership::ownershipId.name).order(SortOrder.DESC))
            .withMaxResults(size)
            .build()
    }
}

data class EsOwnershipByIdFilter(
    val ownershipId: String,
) : EsOwnershipFilter {
    override fun asQuery(): Query =
        NativeSearchQueryBuilder().withQuery(idsQuery().addIds(ownershipId)).build()
}

data class EsOwnershipByIdsFilter(
    val ownershipsIds: Collection<String>,
) : EsOwnershipFilter {
    override fun asQuery(): Query =
        NativeSearchQueryBuilder().withQuery(idsQuery().addIds(*ownershipsIds.toTypedArray())).build()
}

data class EsOwnershipByAuctionOwnershipIdsFilter(
    val ownershipsIds: Collection<String>,
) : EsOwnershipFilter {
    override fun asQuery(): Query {
        val builder = termsQuery(EsOwnership::auctionOwnershipId.name, ownershipsIds)
        return NativeSearchQueryBuilder().withQuery(builder).build()
    }
}

data class EsOwnershipByOwnerFilter(
    val owner: UnionAddress,
    val continuation: DateIdContinuation?,
    val size: Int,
) : EsOwnershipFilter {
    override fun asQuery(): Query {
        val query = termQuery(EsOwnership::owner.name, owner.fullId())
        return genericBuild(query, continuation, size)
    }
}

data class EsOwnershipByItemFilter(
    val itemId: ItemIdDto,
    val continuation: DateIdContinuation?,
    val size: Int,
) : EsOwnershipFilter {
    override fun asQuery(): Query {
        val query = termQuery(EsOwnership::itemId.name, itemId.fullId())
        return genericBuild(query, continuation, size)
    }
}
