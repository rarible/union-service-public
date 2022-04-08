package com.rarible.protocol.union.search.core.repository


import com.rarible.protocol.union.search.core.ElasticOrder
import org.springframework.data.elasticsearch.repository.ReactiveElasticsearchRepository

interface OrderEsRepository: ReactiveElasticsearchRepository<ElasticOrder, String> {

    companion object {
        const val INDEX = "order"
    }
}
