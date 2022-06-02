package com.rarible.protocol.union.search.indexer.repository

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.model.EsOwnershipByAuctionOwnershipIdsFilter
import com.rarible.protocol.union.core.model.EsOwnershipByIdFilter
import com.rarible.protocol.union.core.model.EsOwnershipByIdsFilter
import com.rarible.protocol.union.core.model.EsOwnershipByItemFilter
import com.rarible.protocol.union.core.model.EsOwnershipByOwnerFilter
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.test.context.ContextConfiguration
import randomEsOwnership
import randomOwnershipId
import randomUnionAddress

@IntegrationTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [SearchConfiguration::class])
internal class OwnershipEsRepositoryFt {
    @Autowired
    protected lateinit var repository: EsOwnershipRepository

    @Autowired
    private lateinit var elasticsearchTestBootstrapper: ElasticsearchTestBootstrapper

    @BeforeEach
    fun setUp() = runBlocking<Unit> {
        elasticsearchTestBootstrapper.bootstrap()
    }

    @Test
    fun `should save and read`(): Unit = runBlocking {
        val expected = randomEsOwnership()

        repository.saveAll(listOf(expected)).first()
        val actual = repository.findById(expected.ownershipId)
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `should delete`(): Unit = runBlocking {
        val expected = randomEsOwnership()

        repository.saveAll(listOf(expected)).first()
        assertThat(repository.findById(expected.ownershipId)).isEqualTo(expected)

        repository.deleteAll(listOf(expected.ownershipId))

        repository.refresh()

        val deleted = repository.findById(expected.ownershipId)
        assertNull(deleted)
    }

    @Test
    fun `should find with by id filter`(): Unit = runBlocking {
        val expected = randomEsOwnership()

        repository.saveAll(listOf(expected)).first()

        val filter = EsOwnershipByIdFilter(expected.ownershipId)
        repository.findByFilter(filter).let { actual ->
            assertThat(actual).hasSize(1)
            assertThat(actual.single()).isEqualTo(expected)
        }
    }

    @Test
    fun `should find with by ids filter`(): Unit = runBlocking {
        val expected = listOf(
            randomEsOwnership(),
            randomEsOwnership(),
            randomEsOwnership(),
        ).associateBy { it.ownershipId }

        repository.saveAll(expected.values).first()

        val filter = EsOwnershipByIdsFilter(expected.keys)
        repository.findByFilter(filter).let { actual ->
            assertThat(actual).hasSize(3)
            assertThat(actual.toSet()).isEqualTo(expected.values.toSet())
        }
    }

    @Test
    fun `should find with by auctionOwnershipIds filter`(): Unit = runBlocking {
        val expected = listOf(
            randomEsOwnership(),
            randomEsOwnership(),
            randomEsOwnership(),
        ).associateBy { it.auctionOwnershipId!! }

        repository.saveAll(expected.values).first()

        val filter = EsOwnershipByAuctionOwnershipIdsFilter(expected.keys)
        repository.findByFilter(filter).let { actual ->
            assertThat(actual).hasSize(3)
            assertThat(actual.toSet()).isEqualTo(expected.values.toSet())
        }
    }

    @Test
    fun `should find with by owner filter`(): Unit = runBlocking {
        val id = randomOwnershipId()
        val expected = randomEsOwnership(id)

        repository.saveAll(listOf(expected)).first()

        val filter = EsOwnershipByOwnerFilter(id.owner, null, 50)
        repository.findByFilter(filter).let { actual ->
            println(actual)
            assertThat(actual).hasSize(1)
            assertThat(actual.single()).isEqualTo(expected)
        }
    }

    @Test
    fun `should find with by itemId filter`(): Unit = runBlocking {
        val id = randomOwnershipId()
        val expected = randomEsOwnership(id)

        repository.saveAll(listOf(expected)).first()

        val filter = EsOwnershipByItemFilter(id.getItemId(), null, 50)
        repository.findByFilter(filter).let { actual ->
            println(actual)
            assertThat(actual).hasSize(1)
            assertThat(actual.single()).isEqualTo(expected)
        }
    }

    @Test
    fun `should find with by owner filter + continuation`(): Unit = runBlocking {
        val id = randomOwnershipId()
        val data = (1..10).map {
            randomEsOwnership(id.copy(itemIdValue = randomString()))
        }
        repository.saveAll(data)

        val c1 = repository.findByFilter(EsOwnershipByOwnerFilter(id.owner, null, 3)).let { result ->
            println(null)
            println(result.joinToString("\n"))
            assertThat(result).hasSize(3)
            result.last().let { DateIdContinuation(it.date, it.ownershipId) }
        }

        val c2 = repository.findByFilter(EsOwnershipByOwnerFilter(id.owner, c1, 3)).let { result ->
            println(c1)
            println(result.joinToString("\n"))
            assertThat(result).hasSize(3)
            result.last().let { DateIdContinuation(it.date, it.ownershipId) }
        }

        val c3 = repository.findByFilter(EsOwnershipByOwnerFilter(id.owner, c2, 3)).let { result ->
            println(c2)
            println(result.joinToString("\n"))
            assertThat(result).hasSize(3)
            result.last().let { DateIdContinuation(it.date, it.ownershipId) }
        }

        repository.findByFilter(EsOwnershipByOwnerFilter(id.owner, c3, 3)).let { result ->
            println(c3)
            println(result.joinToString("\n"))
            assertThat(result).hasSize(1)
            result.last().let { DateIdContinuation(it.date, it.ownershipId) }
        }
    }

    @Test
    fun `should find with by itemId filter + continuation`(): Unit = runBlocking {
        val id = randomOwnershipId()
        val data = (1..10).map {
            randomEsOwnership(id.copy(owner = randomUnionAddress(blockchain = id.blockchain, value = randomString())))
        }
        repository.saveAll(data)

        val c1 = repository.findByFilter(EsOwnershipByItemFilter(id.getItemId(), null, 3)).let { result ->
            println(null)
            println(result.joinToString("\n"))
            assertThat(result).hasSize(3)
            result.last().let { DateIdContinuation(it.date, it.ownershipId) }
        }

        val c2 = repository.findByFilter(EsOwnershipByItemFilter(id.getItemId(), c1, 3)).let { result ->
            println(c1)
            println(result.joinToString("\n"))
            assertThat(result).hasSize(3)
            result.last().let { DateIdContinuation(it.date, it.ownershipId) }
        }

        val c3 = repository.findByFilter(EsOwnershipByItemFilter(id.getItemId(), c2, 3)).let { result ->
            println(c2)
            println(result.joinToString("\n"))
            assertThat(result).hasSize(3)
            result.last().let { DateIdContinuation(it.date, it.ownershipId) }
        }

        repository.findByFilter(EsOwnershipByItemFilter(id.getItemId(), c3, 3)).let { result ->
            println(c3)
            println(result.joinToString("\n"))
            assertThat(result).hasSize(1)
            result.last().let { DateIdContinuation(it.date, it.ownershipId) }
        }
    }
}
