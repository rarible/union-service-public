package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.BlockchainDto
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.RangeQueryBuilder
import org.elasticsearch.index.query.TermQueryBuilder
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder

sealed interface EsCollectionFilter {
    fun toQuery(): NativeSearchQuery
}

data class EsCollectionFilterAll(
    val blockchains: Set<BlockchainDto>,
    val cursor: String?,
    val size: Int
): EsCollectionFilter {
    override fun toQuery(): NativeSearchQuery {
        val builder = NativeSearchQueryBuilder()
        val query = BoolQueryBuilder()
        blockchains.forEach {
            query.must(BoolQueryBuilder().should(TermQueryBuilder("blockchain", it.name)).minimumShouldMatch(1))
        }

        if (!cursor.isNullOrEmpty()) {
            query.must(RangeQueryBuilder("collectionId").gt(cursor))
        }

        return builder.withQuery(query).withMaxResults(size).build()
    }
}

data class EsCollectionFilterByOwner(
    val blockchains: Set<BlockchainDto>,
    val owner: String,
    val cursor: String?,
    val size: Int
): EsCollectionFilter {
    override fun toQuery(): NativeSearchQuery {
        val builder = NativeSearchQueryBuilder()
        val query = BoolQueryBuilder()
        blockchains.forEach {
            query.must(BoolQueryBuilder().should(TermQueryBuilder("blockchain", it.name)).minimumShouldMatch(1))
        }

        query.must(TermQueryBuilder("owner", owner))

        if (!cursor.isNullOrEmpty()) {
            query.must(RangeQueryBuilder("collectionId").gt(cursor))
        }

        return builder.withQuery(query).withMaxResults(size).build()

    }
}
