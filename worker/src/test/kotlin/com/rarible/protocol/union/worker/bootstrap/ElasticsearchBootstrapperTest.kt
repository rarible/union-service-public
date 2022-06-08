package com.rarible.protocol.union.worker.bootstrap

import com.rarible.protocol.union.core.elasticsearch.EsEntityMetadataType
import com.rarible.protocol.union.core.elasticsearch.EsMetadataRepository
import com.rarible.protocol.union.core.elasticsearch.EsNameResolver
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.core.elasticsearch.ReindexSchedulingService
import com.rarible.protocol.union.core.elasticsearch.bootstrap.ElasticsearchBootstrapper
import com.rarible.protocol.union.core.elasticsearch.getId
import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinition
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinitionExtended
import com.rarible.protocol.union.worker.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.client.indices.GetIndexRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.RepeatedTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations

@IntegrationTest
internal class ElasticsearchBootstrapperTest {
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

    private lateinit var entityDefinitions: List<EntityDefinition>
    private lateinit var entityDefinitionExtended: EntityDefinitionExtended
    private lateinit var bootstraper: ElasticsearchBootstrapper

    @BeforeEach
    fun init() = runBlocking {
        entityDefinitions = listOf(EsActivity.ENTITY_DEFINITION)
        entityDefinitionExtended = esNameResolver.createEntityDefinitionExtended(EsActivity.ENTITY_DEFINITION)
        bootstraper = ElasticsearchBootstrapper(
            esNameResolver = esNameResolver,
            esOperations = reactiveElasticSearchOperations,
            entityDefinitions = entityDefinitions,
            reindexSchedulingService,
            indexService,
            forceUpdate = emptySet(),
        )

        val elasticsearchTestBootstrapper = ElasticsearchTestBootstrapper(
            esNameResolver = esNameResolver,
            esOperations = reactiveElasticSearchOperations,
            entityDefinitions = entityDefinitions,
            repositories = emptyList()
        )

        elasticsearchTestBootstrapper.removeAllIndexes(entityDefinitionExtended)
    }

    @RepeatedTest(3)
    fun `update index mappings`() = runBlocking<Unit> {

        bootstraper.bootstrap()
        bootstraper.bootstrap()

        val indexName = entityDefinitionExtended.indexName(entityDefinitionExtended.versionData)
        val aliasName = entityDefinitionExtended.aliasName
        val aliasWriteName = entityDefinitionExtended.writeAliasName
        var indexInfo =
            reactiveElasticSearchOperations.execute { it.indices().getIndex(GetIndexRequest(indexName)) }.awaitFirst()
        assertThat(indexInfo.aliases.values.first().first().alias()).isEqualTo(aliasName)
        assertThat(indexInfo.aliases.values.first()[1].alias).isEqualTo(aliasWriteName)
        assertThat(indexInfo.aliases.values.first().size).isEqualTo(2)
        val currentIndexMapping =
            esMetadataRepository.findById(EsEntityMetadataType.MAPPING.getId(entityDefinitionExtended))?.content
        assertThat(currentIndexMapping).isEqualTo(entityDefinitionExtended.mapping)
        val currentIndexSettings =
            esMetadataRepository.findById(EsEntityMetadataType.SETTINGS.getId(entityDefinitionExtended))?.content
        assertThat(currentIndexSettings).isEqualTo(entityDefinitionExtended.settings)
        val currentIndexVersion =
            esMetadataRepository.findById(EsEntityMetadataType.VERSION_DATA.getId(entityDefinitionExtended))?.content
        assertThat(currentIndexVersion).isEqualTo(EsActivity.VERSION.toString())

        // Run one more time without changes
        bootstraper.bootstrap()
        val newVersionData = EsActivity.VERSION + 1
        try {
            reactiveElasticSearchOperations.execute {
                it.indices().getIndex(GetIndexRequest(entityDefinitionExtended.indexName(newVersionData)))
            }.awaitFirst()
            Assertions.fail<String>("Shouldn't be created")
        } catch (_: Exception) {
        }

        // Up version
        val entityDefinitionNew = EntityDefinition(
            entityDefinitionExtended.entity,
            entityDefinitionExtended.mapping,
            newVersionData,
            entityDefinitionExtended.settings
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
        indexInfo =
            reactiveElasticSearchOperations.execute { it.indices().getIndex(GetIndexRequest(indexName)) }.awaitFirst()
        assertThat(indexInfo.aliases.values.first().first().alias()).isEqualTo(aliasName)
        assertThat(indexInfo.aliases.values.first()[1].alias).isEqualTo(aliasWriteName)
        assertThat(indexInfo.aliases.values.first().size).isEqualTo(2)
        val index2Name = entityDefinitionExtended.indexName(newVersionData)
        val index2Info =
            reactiveElasticSearchOperations.execute { it.indices().getIndex(GetIndexRequest(index2Name)) }.awaitFirst()
        assertThat(index2Info.aliases.values.first().first().alias()).isEqualTo(aliasWriteName)
        assertThat(index2Info.aliases.values.first().size).isEqualTo(1)
    }
}