package com.rarible.protocol.union.api.controller.internal

import com.rarible.core.common.nowMillis
import com.rarible.core.task.Task
import com.rarible.core.task.TaskRepository
import com.rarible.core.test.data.randomAddress
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.util.truncatedToSeconds
import com.rarible.protocol.union.enrichment.model.MetaRefreshRequest
import com.rarible.protocol.union.enrichment.repository.MetaRefreshRequestRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.ReactiveMongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.isEqualTo
import org.springframework.data.mongodb.core.query.where
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestTemplate
import scalether.domain.Address
import java.time.Instant

@IntegrationTest
internal class MetaRefreshControllerFt : AbstractIntegrationTest() {
    @Autowired
    private lateinit var metaRefreshRequestRepository: MetaRefreshRequestRepository

    @Autowired
    private lateinit var template: ReactiveMongoTemplate

    @Autowired
    private lateinit var taskRepository: TaskRepository

    private val restTemplate: RestTemplate = RestTemplate()

    @ParameterizedTest
    @MethodSource("testCases")
    fun `schedule full refresh - without scheduledAt`(testCase: TestCase) = runBlocking<Unit> {
        metaRefreshRequestRepository.save(
            MetaRefreshRequest(
                collectionId = "ETHEREUM:${Address.ONE()}",
                full = testCase.existingFull
            )
        )

        val result = restTemplate.postForEntity(
            "$baseUri/${testCase.url}",
            "ETHEREUM:${Address.ONE()}\nPOLYGON:${Address.TWO()}\n\n\"TEZOS:ccc\"\n",
            Void::class.java
        )
        assertThat(result.statusCode).isEqualTo(HttpStatus.OK)

        val collections = metaRefreshRequestRepository.findToScheduleAndUpdate(10)
        assertThat(collections).hasSize(3)
        assertThat(collections[0].collectionId).isEqualTo("ETHEREUM:${Address.ONE()}")
        assertThat(collections[0].full).isEqualTo(testCase.existingFull)
        assertThat(collections[1].collectionId).isEqualTo("POLYGON:${Address.TWO()}")
        assertThat(collections[1].full).isEqualTo(testCase.newFull)
        assertThat(collections[2].collectionId).isEqualTo("TEZOS:ccc")
        assertThat(collections[2].full).isEqualTo(testCase.newFull)
    }

    @ParameterizedTest
    @MethodSource("testCases")
    fun `schedule full refresh - with scheduledAt`(testCase: TestCase) = runBlocking<Unit> {
        val c1 = "ETHEREUM:${randomAddress()}"
        val c2 = "POLYGON:${randomAddress()}"
        val scheduledAt = nowMillis().plusSeconds(100).truncatedToSeconds()

        val exist = MetaRefreshRequest(
            collectionId = c2,
            full = testCase.existingFull,
            createdAt = Instant.now().truncatedToSeconds(),
            scheduledAt = Instant.now().truncatedToSeconds(),
        )
        metaRefreshRequestRepository.save(exist)

        val result = restTemplate.postForEntity(
            "$baseUri/${testCase.url}?scheduledAt=$scheduledAt",
            "$c1\n$c2",
            Void::class.java
        )
        assertThat(result.statusCode).isEqualTo(HttpStatus.OK)

        // Existing Polygon record is NOT modified - so can be scheduled
        val collections = metaRefreshRequestRepository.findToScheduleAndUpdate(10)
        assertThat(collections).hasSize(1)
        assertThat(collections[0]).isEqualTo(exist)

        // New Ethereum inserted, but not triggered yet
        val scheduled = template.findOne(
            Query(where(MetaRefreshRequest::collectionId).isEqualTo(c1)),
            MetaRefreshRequest::class.java
        ).awaitSingle()
        assertThat(scheduled.collectionId).isEqualTo(c1)
        assertThat(scheduled.full).isEqualTo(testCase.newFull)
        assertThat(scheduled.scheduledAt).isEqualTo(scheduledAt)
    }

    @Test
    fun `schedule full refresh - with simplehash`() = runBlocking<Unit> {
        val c1 = "ETHEREUM:${randomAddress()}"
        val result = restTemplate.postForEntity(
            "$baseUri/maintenance/items/meta/refresh/full?withSimpleHash=true",
            c1,
            Void::class.java
        )
        assertThat(result.statusCode).isEqualTo(HttpStatus.OK)

        val scheduled = template.findOne(
            Query(where(MetaRefreshRequest::collectionId).isEqualTo(c1)),
            MetaRefreshRequest::class.java
        ).awaitSingle()
        assertThat(scheduled.collectionId).isEqualTo(c1)
        assertThat(scheduled.full).isEqualTo(true)
        assertThat(scheduled.withSimpleHash).isEqualTo(false)
    }

    @Test
    fun `cancel refresh`() {
        runBlocking {
            metaRefreshRequestRepository.save(
                MetaRefreshRequest(
                    collectionId = "ETHEREUM:0xaaa",
                    full = true
                )
            )
            metaRefreshRequestRepository.save(
                MetaRefreshRequest(
                    collectionId = "ETHEREUM:0x000",
                    full = true
                )
            )
        }

        restTemplate.delete("$baseUri/maintenance/items/meta/refresh/cancel")

        runBlocking {
            assertThat(metaRefreshRequestRepository.countNotScheduled()).isEqualTo(0)
        }
    }

    @Test
    fun statistics() {
        assertThat(
            restTemplate.getForObject(
                "$baseUri/maintenance/items/meta/refresh/status",
                String::class.java
            )
        ).isEqualTo(
            """Queue size: 0
            |Currently processing: """.trimMargin()
        )
        runBlocking {
            metaRefreshRequestRepository.save(
                MetaRefreshRequest(
                    collectionId = "collection1",
                    full = true
                )
            )
            metaRefreshRequestRepository.save(
                MetaRefreshRequest(
                    collectionId = "collection2",
                    full = false
                )
            )
        }

        assertThat(
            restTemplate.getForObject(
                "$baseUri/maintenance/items/meta/refresh/status",
                String::class.java
            )
        ).isEqualTo(
            """Queue size: 2
            |Currently processing: """.trimMargin()
        )

        runBlocking {
            taskRepository.save(
                Task(
                    type = "META_REFRESH_TASK",
                    param = """{"collectionId":"collection3","full":true}""",
                    running = true
                )
            ).awaitSingle()
            taskRepository.save(
                Task(
                    type = "META_REFRESH_TASK",
                    param = """{"collectionId":"collection4","full":true}""",
                    running = true
                )
            ).awaitSingle()
        }

        assertThat(
            testRestTemplate.getForObject(
                "$baseUri/maintenance/items/meta/refresh/status",
                String::class.java
            )
        ).isEqualTo(
            """Queue size: 2
            |Currently processing: collection3, collection4""".trimMargin()
        )
    }

    companion object {
        @JvmStatic
        fun testCases() = listOf(
            TestCase(
                url = "/maintenance/items/meta/refresh/full",
                existingFull = false,
                newFull = true
            ),
            TestCase(
                url = "/maintenance/items/meta/refresh/emptyOnly",
                existingFull = true,
                newFull = false
            )
        )
    }

    data class TestCase(
        val url: String,
        val existingFull: Boolean,
        val newFull: Boolean
    )
}