package com.rarible.protocol.union.search.core.service.query

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.search.core.config.SearchConfiguration
import com.rarible.protocol.union.search.core.model.ActivityCursor
import com.rarible.protocol.union.search.core.model.ActivitySort
import com.rarible.protocol.union.search.core.model.ElasticActivityQueryGenericFilter
import com.rarible.protocol.union.search.core.repository.ActivityEsRepository
import com.rarible.protocol.union.search.test.IntegrationTest
import com.rarible.protocol.union.search.test.buildActivity
import kotlinx.coroutines.reactive.awaitLast
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.test.context.ContextConfiguration
import java.time.Instant

@IntegrationTest
@EnableAutoConfiguration
@ContextConfiguration(classes = [SearchConfiguration::class])
internal class ActivityQueryServiceIntegrationTest {

    @Autowired
    protected lateinit var activityEsRepository: ActivityEsRepository

    @Autowired
    private lateinit var service: ActivityQueryService

    @BeforeEach
    fun setUp() {
        activityEsRepository.deleteAll().block()
    }

    @Test
    fun `should return up to 3 activities from query`() = runBlocking<Unit> {
        // given
        val filter = ElasticActivityQueryGenericFilter(
            blockchains = setOf(BlockchainDto.SOLANA, BlockchainDto.TEZOS)
        )
        val toFind1 = buildActivity().copy(
            blockchain = BlockchainDto.SOLANA,
            date = Instant.ofEpochMilli(5000),
        )
        val toFind2 = buildActivity().copy(
            blockchain = BlockchainDto.TEZOS,
            date = Instant.ofEpochMilli(4000),
        )
        val toFind3 = buildActivity().copy(
            blockchain = BlockchainDto.SOLANA,
            date = Instant.ofEpochMilli(3000),
        )
        val toSkip1 = buildActivity().copy(
            blockchain = BlockchainDto.TEZOS,
            date = Instant.ofEpochMilli(2000),
        )
        val toSkip2 = buildActivity().copy(
            blockchain = BlockchainDto.SOLANA,
            date = Instant.ofEpochMilli(1000),
        )
        activityEsRepository.saveAll(listOf(toSkip1, toFind3, toSkip2, toFind2, toFind1)).awaitLast()

        // when
        val actual = service.query(filter, ActivitySort(true), 3)

        // then
        assertThat(actual.activities).containsExactly(toFind1, toFind2, toFind3)
        val cursor = ActivityCursor.fromString(actual.cursor!!)!!
        assertThat(cursor.date).isEqualTo(toFind3.date)
        assertThat(cursor.blockNumber).isEqualTo(toFind3.blockNumber)
        assertThat(cursor.logIndex).isEqualTo(toFind3.logIndex)
        assertThat(cursor.salt).isEqualTo(toFind3.salt)
    }

    @Test
    fun `should be able to iterate over activities with cursor`() = runBlocking<Unit> {
        // given
        val first = buildActivity().copy(date = Instant.ofEpochMilli(100))
        val second = buildActivity().copy(date = Instant.ofEpochMilli(200))
        val third = buildActivity().copy(date = Instant.ofEpochMilli(300))
        val fourth = buildActivity().copy(date = Instant.ofEpochMilli(400))
        val fifth = buildActivity().copy(date = Instant.ofEpochMilli(500))
        val sixth = buildActivity().copy(date = Instant.ofEpochMilli(600))
        activityEsRepository.saveAll(listOf(second, fourth, sixth, fifth, third, first)).awaitLast()
        var filter = ElasticActivityQueryGenericFilter()

        // when
        val query1 = service.query(filter, ActivitySort(false), 2)
        filter = filter.copy(cursor = query1.cursor)
        val query2 = service.query(filter, ActivitySort(false), 2)
        filter = filter.copy(cursor = query2.cursor)
        val query3 = service.query(filter, ActivitySort(false), 2)
        filter = filter.copy(cursor = query3.cursor)
        val query4 = service.query(filter, ActivitySort(false), 2)

        // then
        assertThat(query1.activities).containsExactly(first, second)
        assertThat(query2.activities).containsExactly(third, fourth)
        assertThat(query3.activities).containsExactly(fifth, sixth)
        assertThat(query4.activities).isEmpty()
        assertThat(query4.cursor).isNull()
    }
}
