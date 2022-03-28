package com.rarible.protocol.union.search.core.repository


import com.rarible.protocol.union.search.core.ElasticActivity
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository
import java.util.UUID


interface ActivityEsRepository: ReactiveElasticsearchRepository<ElasticActivity, UUID> {

    companion object {
        const val INDEX = "activity"
    }
}

