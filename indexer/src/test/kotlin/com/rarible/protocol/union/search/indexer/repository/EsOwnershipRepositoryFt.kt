package com.rarible.protocol.union.search.indexer.repository

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.es.ElasticsearchTestBootstrapper
import com.rarible.protocol.union.core.model.elastic.EsOwnershipByItemFilter
import com.rarible.protocol.union.core.model.elastic.EsOwnershipByOwnerFilter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.configuration.SearchConfiguration
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import okhttp3.internal.toHexString
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
import java.time.Instant

@IntegrationTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [SearchConfiguration::class])
internal class EsOwnershipRepositoryFt {

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
    fun `should do bulk save and delete`() = runBlocking<Unit> {
        // given
        val initial = (1..10).map { randomEsOwnership() }
        val toSave = (1..10).map { randomEsOwnership() }
        repository.saveAll(initial)
        val toDelete = initial.map { it.ownershipId }

        // when
        repository.bulk(toSave, toDelete)

        // then
        toSave.forEach {
            assertThat(repository.findById(it.ownershipId)).isNotNull
        }
        toDelete.forEach {
            assertThat(repository.findById(it)).isNull()
        }
    }

    @Test
    fun `should find with by owner filter`(): Unit = runBlocking {
        // given
        val owner = randomUnionAddress()
        val toFind1 = randomEsOwnership().copy(owner = owner.fullId(), date = Instant.ofEpochSecond(50))
        val toFind2 = randomEsOwnership().copy(owner = owner.fullId(), date = Instant.ofEpochSecond(40))
        val toMiss1 = randomEsOwnership()
        val toMiss2 = randomEsOwnership()
        repository.saveAll(listOf(toFind1, toFind2, toMiss1, toMiss2))

        // when
        val actual = repository.search(EsOwnershipByOwnerFilter(owner)).entities

        // then
        assertThat(actual).containsExactly(toFind1, toFind2)
    }

    @Test
    fun `should find with by itemId filter`(): Unit = runBlocking {
        // given
        val id: ItemIdDto = ItemIdDto(BlockchainDto.FLOW, randomString())
        val toFind1 = randomEsOwnership().copy(itemId = id.fullId(), date = Instant.ofEpochSecond(50))
        val toFind2 = randomEsOwnership().copy(itemId = id.fullId(), date = Instant.ofEpochSecond(40))
        val toMiss1 = randomEsOwnership()
        val toMiss2 = randomEsOwnership()
        repository.saveAll(listOf(toFind1, toFind2, toMiss1, toMiss2))

        // when
        val actual = repository.search(EsOwnershipByItemFilter(id)).entities

        // then
        assertThat(actual).containsExactly(toFind1, toFind2)
    }

    @Test
    fun `should find with by owner filter + continuation`(): Unit = runBlocking {
        val id = randomOwnershipId()
        val data = (0..9).map {
            randomEsOwnership(id.copy(itemIdValue = randomString())).copy(
                // 0 0 0 1 1 1 2 2 2 3 (desc)
                // 0 1 2 3 4 5 6 7 8 9  (asc)
                date = Instant.ofEpochSecond(100 * (it / 3).toLong()),
                ownershipId = it.toHexString()
            )
        }
        repository.bulk(data, emptyList())

        val c1 = repository.search(EsOwnershipByOwnerFilter(owner = id.owner), limit = 3).let { (cursor, result) ->
            println(null)
            println(result.joinToString("\n"))
            assertThat(result).containsExactly(data[9], data[6], data[7])
            cursor
        }

        val c2 = repository.search(EsOwnershipByOwnerFilter(owner = id.owner, cursor = c1), limit = 3)
            .let { (cursor, result) ->
                println(c1)
                println(result.joinToString("\n"))
                assertThat(result).containsExactly(data[8], data[3], data[4])
                cursor
            }

        val c3 = repository.search(EsOwnershipByOwnerFilter(owner = id.owner, cursor = c2), limit = 3)
            .let { (cursor, result) ->
                println(c2)
                println(result.joinToString("\n"))
                assertThat(result).containsExactly(data[5], data[0], data[1])
                cursor
            }

        val c4 = repository.search(EsOwnershipByOwnerFilter(owner = id.owner, cursor = c3), limit = 3)
            .let { (cursor, result) ->
                println(c3)
                println(result.joinToString("\n"))
                assertThat(result).containsExactly(data[2])
                cursor
            }

        repository.search(EsOwnershipByOwnerFilter(owner = id.owner, cursor = c4), limit = 3)
            .let { (cursor, result) ->
                println(c4)
                println(result.joinToString("\n"))
                assertThat(result).isEmpty()
                assertThat(cursor).isNull()
            }
    }

    @Test
    fun `should find with by itemId filter + continuation`(): Unit = runBlocking {
        val id = randomOwnershipId()
        val data = (0..9).map {
            randomEsOwnership(
                id.copy(
                    owner = randomUnionAddress(
                        blockchain = id.blockchain,
                        value = randomString()
                    )
                )
            ).copy(
                // 0 0 0 1 1 1 2 2 2 3 (desc)
                // 0 1 2 3 4 5 6 7 8 9  (asc)
                date = Instant.ofEpochSecond(100 * (it / 3).toLong()),
                ownershipId = it.toHexString()
            )
        }
        repository.bulk(data, emptyList())

        val c1 = repository.search(EsOwnershipByItemFilter(id.getItemId()), limit = 3).let { (cursor, result) ->
            println(null)
            println(result.joinToString("\n"))
            assertThat(result).containsExactly(data[9], data[6], data[7])
            cursor
        }

        val c2 = repository.search(EsOwnershipByItemFilter(id.getItemId(), c1), limit = 3)
            .let { (cursor, result) ->
                println(c1)
                println(result.joinToString("\n"))
                assertThat(result).containsExactly(data[8], data[3], data[4])
                cursor
            }

        val c3 = repository.search(EsOwnershipByItemFilter(id.getItemId(), c2), limit = 3)
            .let { (cursor, result) ->
                println(c2)
                println(result.joinToString("\n"))
                assertThat(result).containsExactly(data[5], data[0], data[1])
                cursor
            }

        val c4 = repository.search(EsOwnershipByItemFilter(id.getItemId(), c3), limit = 3)
            .let { (cursor, result) ->
                println(c3)
                println(result.joinToString("\n"))
                assertThat(result).containsExactly(data[2])
                cursor
            }

        repository.search(EsOwnershipByItemFilter(id.getItemId(), c4), limit = 3)
            .let { (cursor, result) ->
                println(c4)
                println(result.joinToString("\n"))
                assertThat(result).isEmpty()
                assertThat(cursor).isNull()
            }
    }
}
