package com.rarible.protocol.union.worker.task.search

import com.rarible.core.task.TaskRepository
import com.rarible.protocol.union.worker.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirstOrDefault
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
internal class ReindexServiceIt {

    @Autowired
    lateinit var taskRepository: TaskRepository

    @Autowired
    lateinit var reindexService: ReindexService

    @Test
    fun `should schedule activity reindex and alias switch`() = runBlocking<Unit> {
        taskRepository.deleteAll().awaitSingleOrNull()
        reindexService.scheduleActivityReindex("test_activity_index")

        val tasks = taskRepository.findAll().collectList().awaitFirstOrDefault(emptyList())
        Assertions.assertThat(tasks).hasSize(85) //all blockchains * all activities + index switch
    }

    @Test
    fun `should schedule colelction reindex and alias switch`() = runBlocking<Unit> {
        taskRepository.deleteAll().awaitSingleOrNull()
        reindexService.scheduleCollectionReindex("test_collection_index")

        val tasks = taskRepository.findAll().collectList().awaitFirstOrDefault(emptyList())
        Assertions.assertThat(tasks).hasSize(85) //all blockchains * all activities + index switch
    }

}