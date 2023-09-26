package com.rarible.protocol.union.search.indexer.repository

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.core.model.elastic.EsItemSort
import com.rarible.protocol.union.core.model.elastic.EsItemSortType
import com.rarible.protocol.union.core.model.elastic.EsTrait
import com.rarible.protocol.union.core.model.elastic.SortType
import com.rarible.protocol.union.core.model.elastic.TraitSort
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.repository.search.internal.EsItemQuerySortService
import com.rarible.protocol.union.enrichment.test.data.randomEsItem
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.search.sort.SortOrder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder
import org.springframework.test.context.ContextConfiguration

@IntegrationTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [SearchConfiguration::class])
class EsItemQuerySortServiceIntegrationTest {

    @Autowired
    protected lateinit var repository: EsItemRepository

    // Sort by price is tested in EsItemQueryScoreServiceIntegrationTest
    @Autowired
    private lateinit var service: EsItemQuerySortService

    @Autowired
    private lateinit var elasticsearchTestBootstrapper: ElasticsearchTestBootstrapper

    @BeforeEach
    fun setUp() = runBlocking<Unit> {
        elasticsearchTestBootstrapper.bootstrap()
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `should sort by lastUpdatedAt and id`(descending: Boolean) = runBlocking<Unit> {
        // given
        elasticsearchTestBootstrapper.bootstrap()
        val builder = NativeSearchQueryBuilder()

        val sort = if (descending) {
            EsItemSort(type = EsItemSortType.LATEST_FIRST)
        } else {
            EsItemSort(type = EsItemSortType.EARLIEST_FIRST)
        }
        val now = nowMillis()
        val first = randomEsItem().copy(lastUpdatedAt = now.plusSeconds(25), itemId = "A")
        val second = randomEsItem().copy(lastUpdatedAt = now.plusSeconds(25), itemId = "B")
        val third = randomEsItem().copy(lastUpdatedAt = now.plusSeconds(50), itemId = "C")
        val fourth = randomEsItem().copy(lastUpdatedAt = now.plusSeconds(100), itemId = "D")
        val fifth = randomEsItem().copy(lastUpdatedAt = now.plusSeconds(100), itemId = "E")
        repository.saveAll(listOf(first, second, third, fourth, fifth).shuffled())
        val expected = if (descending) {
            listOf(fifth, fourth, third, second, first).map { it.itemId }
        } else {
            listOf(first, second, third, fourth, fifth).map { it.itemId }
        }

        // when
        service.applySort(builder, sort)
        val actual = repository.search(builder.build())
            .map { it.content.itemId }

        // then
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `should sort by trait`() = runBlocking<Unit> {
        elasticsearchTestBootstrapper.bootstrap()

        val first = randomEsItem().copy(
            traits = listOf(
                EsTrait(key = "trait1", value = "1"),
                EsTrait(key = "trait2", value = "10")
            ),
            itemId = "A"
        )
        val second = randomEsItem().copy(
            traits = listOf(
                EsTrait(key = "trait1", value = "10"),
                EsTrait(key = "trait2", value = "1")
            ),
            itemId = "B"
        )
        val third = randomEsItem().copy(
            traits = listOf(
                EsTrait(key = "trait1", value = "5"),
                EsTrait(key = "trait2", value = "5")
            ),
            itemId = "C"
        )
        val fourth = randomEsItem().copy(
            traits = listOf(
                EsTrait(key = "trait1", value = "5"),
                EsTrait(key = "trait2", value = "5")
            ),
            itemId = "D"
        )
        val fifth = randomEsItem().copy(
            traits = listOf(
                EsTrait(key = "trait1", value = "6"),
                EsTrait(key = "trait2", value = "3")
            ),
            itemId = "E"
        )
        repository.saveAll(listOf(first, second, third, fourth, fifth).shuffled())

        val builder1 = NativeSearchQueryBuilder()
        service.applySort(
            builder1, EsItemSort(
                type = EsItemSortType.TRAIT,
                traitSort = TraitSort(
                    key = "trait1",
                    sortType = SortType.TEXT,
                    sortOrder = SortOrder.ASC
                )
            )
        )

        assertThat(repository.search(builder1.build()).map { it.content.itemId }).containsExactly(
            first.itemId,
            second.itemId,
            third.itemId,
            fourth.itemId,
            fifth.itemId,
        )

        val builder2 = NativeSearchQueryBuilder()
        service.applySort(
            builder2, EsItemSort(
                type = EsItemSortType.TRAIT,
                traitSort = TraitSort(
                    key = "trait1",
                    sortType = SortType.NUMERIC,
                    sortOrder = SortOrder.ASC
                )
            )
        )

        assertThat(repository.search(builder2.build()).map { it.content.itemId }).containsExactly(
            first.itemId,
            third.itemId,
            fourth.itemId,
            fifth.itemId,
            second.itemId,
        )

        val builder3 = NativeSearchQueryBuilder()
        service.applySort(
            builder3, EsItemSort(
                type = EsItemSortType.TRAIT,
                traitSort = TraitSort(
                    key = "trait1",
                    sortType = SortType.TEXT,
                    sortOrder = SortOrder.DESC
                )
            )
        )

        assertThat(repository.search(builder3.build()).map { it.content.itemId }).containsExactly(
            fifth.itemId,
            fourth.itemId,
            third.itemId,
            second.itemId,
            first.itemId,
        )
    }
}
