package com.rarible.protocol.union.search.core.repository


import com.rarible.protocol.union.search.core.ElasticActivity
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository

interface ActivityEsRepository: ReactiveElasticsearchRepository<ElasticActivity, String> {

    companion object {
        const val INDEX = "activity"
    }
}

