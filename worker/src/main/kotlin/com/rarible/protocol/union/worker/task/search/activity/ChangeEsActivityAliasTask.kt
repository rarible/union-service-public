package com.rarible.protocol.union.worker.task.search.activity

import com.rarible.core.task.TaskRepository
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.worker.task.search.ChangeEsAliasTask
import com.rarible.protocol.union.worker.task.search.ParamFactory
import org.springframework.stereotype.Component

@Component
class ChangeEsActivityAliasTask(
    taskRepository: TaskRepository,
    esActivityRepository: EsActivityRepository,
    indexService: IndexService,
    paramFactory: ParamFactory
) : ChangeEsAliasTask(
    TYPE,
    esActivityRepository.entityDefinition,
    taskRepository,
    esActivityRepository,
    indexService,
    paramFactory
) {
    companion object {
        const val TYPE = "CHANGE_ES_ACTIVITY_ALIAS_TASK"
    }
}