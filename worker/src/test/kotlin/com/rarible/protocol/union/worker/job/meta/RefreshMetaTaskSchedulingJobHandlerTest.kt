package com.rarible.protocol.union.worker.job.meta

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.task.Task
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.union.worker.task.meta.RefreshMetaTask
import com.rarible.protocol.union.worker.task.meta.RefreshMetaTaskParam
import com.rarible.protocol.union.enrichment.model.MetaRefreshRequest
import com.rarible.protocol.union.enrichment.repository.MetaRefreshRequestRepository
import com.rarible.protocol.union.worker.AbstractIntegrationTest
import com.rarible.protocol.union.worker.IntegrationTest
import com.rarible.protocol.union.worker.config.WorkerProperties
import com.rarible.protocol.union.worker.kafka.LagService
import com.rarible.protocol.union.worker.metrics.MetaRefreshMetrics
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
internal class RefreshMetaTaskSchedulingJobHandlerTest : AbstractIntegrationTest() {
    @Autowired
    private lateinit var taskRepository: TaskRepository

    @Autowired
    private lateinit var collectionMetaRefreshSchedulingService: CollectionMetaRefreshSchedulingService

    @Autowired
    private lateinit var metaRefreshRequestRepository: MetaRefreshRequestRepository

    @Autowired
    private lateinit var metaRefreshMetrics: MetaRefreshMetrics

    private lateinit var refreshMetaTaskSchedulingJobHandler: RefreshMetaTaskSchedulingJobHandler

    private lateinit var lagService: LagService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun before() {
        lagService = mockk()
        refreshMetaTaskSchedulingJobHandler = RefreshMetaTaskSchedulingJobHandler(
            taskRepository = taskRepository,
            properties = WorkerProperties(),
            metaRefreshRequestRepository = metaRefreshRequestRepository,
            lagService = lagService,
            collectionMetaRefreshSchedulingService = collectionMetaRefreshSchedulingService,
            metaRefreshMetrics = metaRefreshMetrics,
        )
    }

    @Test
    fun `too many tasks`() = runBlocking<Unit> {
        (1..10).forEach {
            taskRepository.save(
                Task(
                    type = RefreshMetaTask.META_REFRESH_TASK,
                    param = "$it",
                    running = true
                )
            ).awaitSingle()
        }
        metaRefreshRequestRepository.save(
            MetaRefreshRequest(
                collectionId = "test",
                full = true
            )
        )

        refreshMetaTaskSchedulingJobHandler.handle()

        assertThat(taskRepository.count().awaitSingle()).isEqualTo(10)
        assertThat(metaRefreshRequestRepository.findToScheduleAndUpdate(1)).isNotEmpty
    }

    @Test
    fun `start new tasks`() = runBlocking<Unit> {
        coEvery { lagService.isLagOk() } returns true
        val runningTasks = (1..8).map {
            taskRepository.save(
                Task(
                    type = RefreshMetaTask.META_REFRESH_TASK,
                    param = "$it",
                    running = true
                )
            ).awaitSingle()
        }

        taskRepository.save(
            Task(
                type = RefreshMetaTask.META_REFRESH_TASK,
                param = "666",
                running = false,
                lastStatus = TaskStatus.COMPLETED
            )
        ).awaitSingle()

        metaRefreshRequestRepository.save(
            MetaRefreshRequest(
                collectionId = "test1",
                full = true
            )
        )

        metaRefreshRequestRepository.save(
            MetaRefreshRequest(
                collectionId = "test2",
                full = false
            )
        )

        metaRefreshRequestRepository.save(
            MetaRefreshRequest(
                collectionId = "test3",
                full = false
            )
        )

        refreshMetaTaskSchedulingJobHandler.handle()

        assertThat(taskRepository.count().awaitSingle()).isEqualTo(10)
        assertThat(
            metaRefreshRequestRepository.findToScheduleAndUpdate(1)[0].collectionId
        ).isEqualTo("test3")
        val tasks = taskRepository.findAll().asFlow().toList()
        assertThat(tasks).containsAll(runningTasks)
        val newTasks = tasks - runningTasks
        val params = newTasks.map { it.param }
        assertThat(params).containsExactlyInAnyOrder(
            objectMapper.writeValueAsString(
                RefreshMetaTaskParam(
                    collectionId = "test1",
                    full = true
                )
            ),
            objectMapper.writeValueAsString(
                RefreshMetaTaskParam(
                    collectionId = "test2",
                    full = false
                )
            )
        )
    }

    @Test
    fun `lag is too big`() = runBlocking<Unit> {
        coEvery { lagService.isLagOk() } returns false
        metaRefreshRequestRepository.save(
            MetaRefreshRequest(
                collectionId = "test",
                full = true
            )
        )

        refreshMetaTaskSchedulingJobHandler.handle()

        assertThat(taskRepository.count().awaitSingle()).isEqualTo(0)
        assertThat(metaRefreshRequestRepository.findToScheduleAndUpdate(1)).isNotEmpty
    }
}