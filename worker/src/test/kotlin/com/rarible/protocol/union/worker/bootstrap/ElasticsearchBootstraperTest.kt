package com.rarible.protocol.union.worker.bootstrap

import com.rarible.protocol.union.core.elasticsearch.EsEntityMetadataType
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.core.elasticsearch.ReindexSchedulingService
import com.rarible.protocol.union.core.elasticsearch.bootstrap.ElasticsearchBootstrapper
import com.rarible.protocol.union.core.elasticsearch.getId
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinition
import com.rarible.protocol.union.core.elasticsearch.EsMetadataRepository
import com.rarible.protocol.union.worker.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.client.indices.GetIndexRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations

@IntegrationTest
internal class ElasticsearchBootstraperTest {
    @Autowired
    private lateinit var reactiveElasticSearchOperations: ReactiveElasticsearchOperations

    @Autowired
    private lateinit var esMetadataRepository: EsMetadataRepository

    @Autowired
    private lateinit var reindexSchedulingService: ReindexSchedulingService

    @Autowired
    private lateinit var esNameResolver: EsNameResolver

    @Autowired
    private lateinit var indexService: IndexService

    @Test
    fun `update index mappings`() = runBlocking<Unit> {

        val entityDefinitions = listOf(EsActivity.ENTITY_DEFINITION)
        val entityDefinition = esNameResolver.createEntityDefinitionExtended(EsActivity.ENTITY_DEFINITION)

        val bootstraper = ElasticsearchBootstrapper(
            esNameResolver = esNameResolver,
            esOperations = reactiveElasticSearchOperations,
            entityDefinitions = entityDefinitions,
            reindexSchedulingService,
            indexService,
            forceUpdate = emptySet(),
        )

        bootstraper.bootstrap()
        bootstraper.bootstrap()

        val indexName = entityDefinition.indexName(entityDefinition.versionData)
        val aliasName = entityDefinition.aliasName
        val aliasWriteName = entityDefinition.writeAliasName
        var indexInfo = reactiveElasticSearchOperations.execute { it.indices().getIndex(GetIndexRequest(indexName)) }
            .awaitFirst()
        assertThat(indexInfo.aliases.values.first().first().alias()).isEqualTo(aliasName)
        assertThat(indexInfo.aliases.values.first()[1].alias).isEqualTo(aliasWriteName)
        assertThat(indexInfo.aliases.values.first().size).isEqualTo(2)
        val currentIndexMapping =
            esMetadataRepository.findById(EsEntityMetadataType.MAPPING.getId(entityDefinition))?.content
        assertThat(currentIndexMapping).isEqualTo(entityDefinition.mapping)
        val currentIndexSettings =
            esMetadataRepository.findById(EsEntityMetadataType.SETTINGS.getId(entityDefinition))?.content
        assertThat(currentIndexSettings).isEqualTo(entityDefinition.settings)
        val currentIndexVersion =
            esMetadataRepository.findById(EsEntityMetadataType.VERSION_DATA.getId(entityDefinition))?.content
        assertThat(currentIndexVersion).isEqualTo(EsActivity.VERSION.toString())

        // Run one more time without changes
        bootstraper.bootstrap()
        val newVersionData = EsActivity.VERSION + 1
        try {
            reactiveElasticSearchOperations.execute {
                it.indices().getIndex(GetIndexRequest(entityDefinition.indexName(newVersionData)))
            }.awaitFirst()
            Assertions.fail<String>("Shouldn't be created")
        } catch (_: Exception) {
        }

        // Up version
        val entityDefinitionNew = EntityDefinition(
            entityDefinition.entity, entityDefinition.mapping, newVersionData, entityDefinition.settings
        )
        val bootstrapperNew = ElasticsearchBootstrapper(
            esNameResolver = esNameResolver,
            esOperations = reactiveElasticSearchOperations,
            entityDefinitions = listOf(entityDefinitionNew),
            reindexSchedulingService,
            indexService,
            forceUpdate = emptySet()
        )
        bootstrapperNew.bootstrap()
        indexInfo = reactiveElasticSearchOperations.execute { it.indices().getIndex(GetIndexRequest(indexName)) }
            .awaitFirst()
        assertThat(indexInfo.aliases.values.first().first().alias()).isEqualTo(aliasName)
        assertThat(indexInfo.aliases.values.first()[1].alias).isEqualTo(aliasWriteName)
        assertThat(indexInfo.aliases.values.first().size).isEqualTo(2)
        val index2Name = entityDefinition.indexName(newVersionData)
        val index2Info = reactiveElasticSearchOperations.execute { it.indices().getIndex(GetIndexRequest(index2Name)) }
            .awaitFirst()
        assertThat(index2Info.aliases.values.first().first().alias()).isEqualTo(aliasWriteName)
        assertThat(index2Info.aliases.values.first().size).isEqualTo(1)
    }
}