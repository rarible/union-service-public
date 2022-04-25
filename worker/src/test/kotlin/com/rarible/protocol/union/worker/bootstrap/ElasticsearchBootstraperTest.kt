package com.rarible.protocol.union.worker.bootstrap

import com.rarible.protocol.union.core.elasticsearch.EsEntityMetadataType
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.elasticsearch.ReindexSchedulingService
import com.rarible.protocol.union.core.elasticsearch.bootstrap.ElasticsearchBootstraper
import com.rarible.protocol.union.core.elasticsearch.bootstrap.indexSettings
import com.rarible.protocol.union.core.elasticsearch.getId
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinition
import com.rarible.protocol.union.core.model.elasticsearch.EsEntitiesConfig.createEsEntities
import com.rarible.protocol.union.worker.IntegrationTest
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.client.indices.GetIndexRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
internal class ElasticsearchBootstraperTest {
    @Autowired
    private lateinit var client: RestHighLevelClient

    @Autowired
    private lateinit var reindexSchedulingService: ReindexSchedulingService

    @Autowired
    private lateinit var esNameResolver: EsNameResolver

    @Test
    fun `update index mappings`() {

        val settings = indexSettings()
        val entityDefinitions = createEsEntities().subList(0, 1)
        val entityDefinition = esNameResolver.createEntityDefinitionExtended(entityDefinitions.first())

        val bootstraper = ElasticsearchBootstraper(
            esNameResolver = esNameResolver,
            client = client,
            entityDefinitions = entityDefinitions,
            reindexSchedulingService,
            forceUpdate = emptySet()
        )

        bootstraper.bootstrap()

        val indexName = entityDefinition.indexName(1)
        val aliasName = entityDefinition.aliasName
        val aliasWriteName = entityDefinition.writeAliasName
        var indexInfo = client.indices().get(GetIndexRequest(indexName), RequestOptions.DEFAULT)
        assertThat(indexInfo.aliases.values.first().first().alias()).isEqualTo(aliasName)
        assertThat(indexInfo.aliases.values.first()[1].alias).isEqualTo(aliasWriteName)
        assertThat(indexInfo.aliases.values.first().size).isEqualTo(2)
        val currentIndexMapping =
            client.get(
                GetRequest(
                    esNameResolver.metadataIndexName(), EsEntityMetadataType.MAPPING.getId(entityDefinition)
                ), RequestOptions.DEFAULT
            )
        assertThat(currentIndexMapping.source["content"]).isEqualTo(entityDefinition.mapping)
        val currentIndexSettings =
            client.get(
                GetRequest(
                    esNameResolver.metadataIndexName(), EsEntityMetadataType.SETTINGS.getId(entityDefinition)
                ), RequestOptions.DEFAULT
            )
        assertThat(currentIndexSettings.source["content"]).isEqualTo(settings)
        val currentIndexVersion =
            client.get(
                GetRequest(
                    esNameResolver.metadataIndexName(), EsEntityMetadataType.VERSION_DATA.getId(entityDefinition)
                ), RequestOptions.DEFAULT
            )
        assertThat(currentIndexVersion.source["content"]).isEqualTo(1)

        // Run one more time without changes
        bootstraper.bootstrap()

        try {
            client.indices().get(GetIndexRequest(entityDefinition.indexName(2)), RequestOptions.DEFAULT)
            Assertions.fail<String>("Shouldn't be created")
        } catch (_: Exception) {
        }

        // Up version
        val entityDefinitionNew = EntityDefinition(entityDefinition.name, entityDefinition.mapping, 2)

        val bootstraperNew = ElasticsearchBootstraper(
            esNameResolver = esNameResolver,
            client = client,
            entityDefinitions = listOf(entityDefinitionNew),
            reindexSchedulingService,
            forceUpdate = emptySet()
        )

        bootstraperNew.bootstrap()

        indexInfo = client.indices().get(GetIndexRequest(indexName), RequestOptions.DEFAULT)
        assertThat(indexInfo.aliases.values.first().first().alias()).isEqualTo(aliasName)
        assertThat(indexInfo.aliases.values.first()[1].alias).isEqualTo(aliasWriteName)
        assertThat(indexInfo.aliases.values.first().size).isEqualTo(2)
        val index2Name = entityDefinition.indexName(2)
        val index2Info = client.indices().get(GetIndexRequest(index2Name), RequestOptions.DEFAULT)
        assertThat(index2Info.aliases.values.first().first().alias()).isEqualTo(aliasWriteName)
        assertThat(index2Info.aliases.values.first().size).isEqualTo(1)
    }
}