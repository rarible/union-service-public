package com.rarible.protocol.union.worker.task.search.trait

import com.rarible.core.task.TaskRepository
import com.rarible.protocol.union.core.elasticsearch.IndexService
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.enrichment.repository.search.EsTraitRepository
import com.rarible.protocol.union.worker.task.search.ChangeEsAliasTask
import com.rarible.protocol.union.worker.task.search.ParamFactory
import org.springframework.stereotype.Component

@Component
class ChangeEsTraitAliasTask(
    taskRepository: TaskRepository,
    esTraitRepository: EsTraitRepository,
    indexService: IndexService,
    paramFactory: ParamFactory
) : ChangeEsAliasTask(
    esTraitRepository.entityDefinition,
    taskRepository,
    esTraitRepository,
    indexService,
    paramFactory
)
