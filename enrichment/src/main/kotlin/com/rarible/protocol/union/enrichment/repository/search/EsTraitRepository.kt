package com.rarible.protocol.union.enrichment.repository.search

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.model.elastic.EsItem
import com.rarible.protocol.union.core.model.elastic.EsTraitStat
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.convert.ElasticsearchConverter
import org.springframework.stereotype.Component

@Component
class EsTraitRepository(
    private val elasticsearchConverter: ElasticsearchConverter,
    objectMapper: ObjectMapper,
    esOperations: ReactiveElasticsearchOperations,
    elasticClient: ReactiveElasticsearchClient,
    esNameResolver: EsNameResolver
) : ElasticSearchRepository<EsTraitStat>(
    objectMapper,
    esOperations,
    esNameResolver.createEntityDefinitionExtended(EsItem.ENTITY_DEFINITION),
    elasticsearchConverter,
    elasticClient,
    EsTraitStat::class.java,
    EsItem::itemId.name,
) {

    override fun entityId(entity: EsTraitStat): String {
        return entity.id
    }
}
