package com.rarible.protocol.union.enrichment.repository.search.internal

import com.rarible.protocol.union.core.model.ElasticActivityFilter
import com.rarible.protocol.union.core.model.ElasticActivityQueryGenericFilter
import com.rarible.protocol.union.core.model.ElasticActivityQueryPerTypeFilter
import com.rarible.protocol.union.core.model.ElasticItemFilter
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.EsActivitySort
import com.rarible.protocol.union.core.model.EsItemSort
import com.rarible.protocol.union.core.model.cursor
import org.elasticsearch.index.query.BoolQueryBuilder
import org.elasticsearch.index.query.MatchQueryBuilder
import org.elasticsearch.index.query.RangeQueryBuilder
import org.elasticsearch.index.query.TermsQueryBuilder
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.stereotype.Component
import scala.collection.`package`

@Component
class EsItemBuilderService {

    fun build(filter: ElasticItemFilter, sort: EsItemSort): NativeSearchQuery {
        val builder = NativeSearchQueryBuilder()
        val query = BoolQueryBuilder()
        builder.withQuery(query)
        return builder.build()
    }
}
