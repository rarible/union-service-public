package com.rarible.protocol.union.api.service.elastic

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.task.Task
import com.rarible.core.task.TaskRepository
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.core.task.ActivityTaskParam
import com.rarible.protocol.union.core.task.ItemTaskParam
import com.rarible.protocol.union.core.task.OwnershipTaskParam
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired


@IntegrationTest
class ElasticMaintenanceServiceIt {

    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Autowired
    private lateinit var service: ElasticMaintenanceService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should schedule reindex activity tasks`() = runBlocking<Unit> {
        // given
        val previousTaskToDelete = Task(
            type = "ACTIVITY_REINDEX",
            param = """
                {
                    "otherData": 42,
                    "tags": ["MAINTENANCE"]
                }
            """.trimIndent(),
            running = false,
        )
        val otherTask = Task(
            type = "ACTIVITY_REINDEX",
            param = """
                {
                    "otherData": "Something else with MAINTENANCE keyword",
                    "tags": []
                }
            """.trimIndent(),
            running = false,
        )
        val saved = taskRepository.saveAll(listOf(previousTaskToDelete, otherTask))
            .collectList().awaitFirst()
        val index = "some_activity_index"
        val blockchains = listOf(BlockchainDto.POLYGON, BlockchainDto.FLOW)
        val types = listOf(ActivityTypeDto.LIST, ActivityTypeDto.MINT)
        val from = 100L
        val to = 200L

        // when
        service.scheduleReindexActivitiesTasks(
            blockchains = blockchains,
            types = types,
            esIndex = index,
            from = from,
            to = to,
        )
        val actual = taskRepository.findByTypeAndParamRegex("ACTIVITY_REINDEX", ".*$index.*")
            .collectList().awaitFirst()

        // then
        assertThat(actual).hasSize(4)
        val allParams = actual.map { objectMapper.readValue(it.param, ActivityTaskParam::class.java) }
        assertThat(allParams).allMatch { it.index == index }
        assertThat(allParams).allMatch { it.from == from }
        assertThat(allParams).allMatch { it.to == to }
        assertThat(allParams).allMatch { it.tags?.contains("MAINTENANCE") ?: false }
        assertThat(allParams.filter { it.blockchain == BlockchainDto.POLYGON }).hasSize(2)
        assertThat(allParams.filter { it.blockchain == BlockchainDto.FLOW }).hasSize(2)
        assertThat(allParams.filter { it.type == ActivityTypeDto.LIST }).hasSize(2)
        assertThat(allParams.filter { it.type == ActivityTypeDto.MINT }).hasSize(2)

        assertThat(taskRepository.findById(saved[0].id.toString()).awaitFirstOrNull()).isNull()
        assertThat(taskRepository.findById(saved[1].id.toString()).awaitFirstOrNull()).isNotNull()
    }

    @Test
    fun `should schedule reindex ownership tasks`() = runBlocking<Unit> {
        // given
        val previousTaskToDelete = Task(
            type = "OWNERSHIP_REINDEX",
            param = """
                {
                    "otherData": 42,
                    "tags": ["MAINTENANCE"]
                }
            """.trimIndent(),
            running = false,
        )
        val otherTask = Task(
            type = "OWNERSHIP_REINDEX",
            param = """
                {
                    "otherData": "Something else with MAINTENANCE keyword",
                    "tags": []
                }
            """.trimIndent(),
            running = false,
        )
        val saved = taskRepository.saveAll(listOf(previousTaskToDelete, otherTask))
            .collectList().awaitFirst()
        val index = "some_ownership_index"
        val blockchains = listOf(BlockchainDto.POLYGON, BlockchainDto.FLOW)
        val from = 100L
        val to = 200L

        // when
        service.scheduleReindexOwnershipsTasks(
            blockchains = blockchains,
            esIndex = index,
            from = from,
            to = to,
        )
        val actual = taskRepository.findByTypeAndParamRegex("OWNERSHIP_REINDEX", ".*$index.*")
            .collectList().awaitFirst()

        // then
        assertThat(actual).hasSize(2)
        val allParams = actual.map { objectMapper.readValue(it.param, OwnershipTaskParam::class.java) }
        assertThat(allParams).allMatch { it.index == index }
        assertThat(allParams).allMatch { it.from == from }
        assertThat(allParams).allMatch { it.to == to }
        assertThat(allParams).allMatch { it.target == OwnershipTaskParam.Target.OWNERSHIP }
        assertThat(allParams).allMatch { it.tags?.contains("MAINTENANCE") ?: false }
        assertThat(allParams.filter { it.blockchain == BlockchainDto.POLYGON }).hasSize(1)
        assertThat(allParams.filter { it.blockchain == BlockchainDto.FLOW }).hasSize(1)

        assertThat(taskRepository.findById(saved[0].id.toString()).awaitFirstOrNull()).isNull()
        assertThat(taskRepository.findById(saved[1].id.toString()).awaitFirstOrNull()).isNotNull()
    }

    @Test
    fun `should schedule reindex item tasks`() = runBlocking<Unit> {
        // given
        val previousTaskToDelete = Task(
            type = "ITEM_REINDEX",
            param = """
                {
                    "otherData": 42,
                    "tags": ["MAINTENANCE"]
                }
            """.trimIndent(),
            running = false,
        )
        val otherTask = Task(
            type = "ITEM_REINDEX",
            param = """
                {
                    "otherData": "Something else with MAINTENANCE keyword",
                    "tags": []
                }
            """.trimIndent(),
            running = false,
        )
        val saved = taskRepository.saveAll(listOf(previousTaskToDelete, otherTask))
            .collectList().awaitFirst()
        val index = "some_item_index"
        val blockchains = listOf(BlockchainDto.POLYGON, BlockchainDto.FLOW)
        val from = 100L
        val to = 200L

        // when
        service.scheduleReindexItemsTasks(
            blockchains = blockchains,
            esIndex = index,
            from = from,
            to = to,
        )
        val actual = taskRepository.findByTypeAndParamRegex("ITEM_REINDEX", ".*$index.*")
            .collectList().awaitFirst()

        // then
        assertThat(actual).hasSize(2)
        val allParams = actual.map { objectMapper.readValue(it.param, ItemTaskParam::class.java) }
        assertThat(allParams).allMatch { it.index == index }
        assertThat(allParams).allMatch { it.from == from }
        assertThat(allParams).allMatch { it.to == to }
        assertThat(allParams).allMatch { it.tags?.contains("MAINTENANCE") ?: false }
        assertThat(allParams.filter { it.blockchain == BlockchainDto.POLYGON }).hasSize(1)
        assertThat(allParams.filter { it.blockchain == BlockchainDto.FLOW }).hasSize(1)

        assertThat(taskRepository.findById(saved[0].id.toString()).awaitFirstOrNull()).isNull()
        assertThat(taskRepository.findById(saved[1].id.toString()).awaitFirstOrNull()).isNotNull()
    }
}
