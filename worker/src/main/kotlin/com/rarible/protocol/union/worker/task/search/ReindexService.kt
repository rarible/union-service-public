package com.rarible.protocol.union.worker.task.search

import com.rarible.core.common.mapAsync
import com.rarible.core.logging.Logger
import com.rarible.core.task.Task
import com.rarible.core.task.TaskRepository
import com.rarible.core.task.TaskStatus
import com.rarible.protocol.union.core.elasticsearch.ReindexSchedulingService
import com.rarible.protocol.union.core.model.elastic.EntityDefinitionExtended
import com.rarible.protocol.union.core.model.elastic.EsActivity
import com.rarible.protocol.union.core.model.elastic.EsCollection
import com.rarible.protocol.union.core.model.elastic.EsEntity
import com.rarible.protocol.union.core.model.elastic.EsItem
import com.rarible.protocol.union.core.model.elastic.EsOrder
import com.rarible.protocol.union.core.model.elastic.EsOwnership
import com.rarible.protocol.union.core.model.elastic.EsTrait
import com.rarible.protocol.union.core.service.router.ActiveBlockchainProvider
import com.rarible.protocol.union.core.task.ActivityTaskParam
import com.rarible.protocol.union.core.task.CollectionTaskParam
import com.rarible.protocol.union.core.task.ItemTaskParam
import com.rarible.protocol.union.core.task.OrderTaskParam
import com.rarible.protocol.union.core.task.OwnershipTaskParam
import com.rarible.protocol.union.core.task.RawTaskParam
import com.rarible.protocol.union.core.task.SyncScope
import com.rarible.protocol.union.core.task.SyncTraitJobParam
import com.rarible.protocol.union.dto.SyncTypeDto
import com.rarible.protocol.union.worker.config.WorkerProperties
import com.rarible.protocol.union.worker.task.search.ChangeEsAliasTask.Companion.getChangeAliasTaskName
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Component

@Component
class ReindexService(
    workerProperties: WorkerProperties,
    private val taskRepository: TaskRepository,
    private val paramFactory: ParamFactory,
    private val activeBlockchainProvider: ActiveBlockchainProvider
) : ReindexSchedulingService {

    private val searchReindexProperties = workerProperties.searchReindex

    override suspend fun scheduleReindex(
        newIndexName: String,
        entityDefinition: EntityDefinitionExtended
    ) {
        when (entityDefinition.entity) {
            EsEntity.ACTIVITY -> {
                scheduleActivityReindex(newIndexName, entityDefinition)
            }

            EsEntity.ITEM -> {
                scheduleItemReindex(newIndexName, entityDefinition)
            }

            EsEntity.COLLECTION -> {
                scheduleCollectionReindex(newIndexName, entityDefinition)
            }

            EsEntity.ORDER -> {
                scheduleOrderReindex(newIndexName, entityDefinition)
            }

            EsEntity.OWNERSHIP -> {
                scheduleOwnershipReindex(newIndexName, entityDefinition)
            }

            EsEntity.TRAIT -> {
                scheduleTraitReindex(newIndexName, entityDefinition)
            }
        }
    }

    private suspend fun scheduleCollectionReindex(indexName: String, definition: EntityDefinitionExtended) {
        val blockchains = activeBlockchainProvider.blockchains
        val taskParams = blockchains.map {
            paramFactory.toString(
                CollectionTaskParam(
                    versionData = definition.versionData,
                    settingsHash = definition.settingsHash,
                    blockchain = it,
                    index = indexName
                )
            )
        }
        val tasks = tasks(EsCollection.ENTITY_DEFINITION.reindexTask, taskParams)

        val changeAliasTaskParam = ChangeAliasTaskParam(
            indexName, taskParams
        )

        val indexSwitch = indexSwitchTask(
            entityName = EsEntity.COLLECTION.entityName,
            changeAliasTaskParam = changeAliasTaskParam,
            definition
        )

        taskRepository.saveAll(tasks + indexSwitch).collectList().awaitFirst()
    }

    private suspend fun scheduleActivityReindex(indexName: String, definition: EntityDefinitionExtended) {
        val blockchains = activeBlockchainProvider.blockchains
        val types = SyncTypeDto.values()
        val taskParams = blockchains.flatMap { blockchain ->
            types.map { type ->
                paramFactory.toString(
                    ActivityTaskParam(
                        versionData = definition.versionData,
                        settingsHash = definition.settingsHash,
                        blockchain = blockchain,
                        type = type,
                        index = indexName
                    )
                )
            }
        }

        val tasks = tasks(EsActivity.ENTITY_DEFINITION.reindexTask, taskParams)

        val changeAliasTaskParam = ChangeAliasTaskParam(
            indexName, taskParams
        )

        val indexSwitch = indexSwitchTask(
            entityName = EsEntity.ACTIVITY.entityName,
            changeAliasTaskParam = changeAliasTaskParam,
            definition
        )
        taskRepository.saveAll(tasks + indexSwitch).collectList().awaitFirst()
    }

    private suspend fun scheduleItemReindex(indexName: String, definition: EntityDefinitionExtended) {
        val blockchains = activeBlockchainProvider.blockchains
        logger.info("Active blockchains {}", blockchains)
        val taskParams = blockchains.map {
            paramFactory.toString(
                ItemTaskParam(
                    versionData = definition.versionData,
                    settingsHash = definition.settingsHash,
                    blockchain = it,
                    index = indexName
                )
            )
        }
        val tasks = tasks(EsItem.ENTITY_DEFINITION.reindexTask, taskParams)
        val changeEsItemAliasTaskParam = ChangeAliasTaskParam(
            indexName, taskParams
        )
        val indexSwitch = indexSwitchTask(
            entityName = EsEntity.ITEM.entityName,
            changeAliasTaskParam = changeEsItemAliasTaskParam,
            definition
        )
        taskRepository.saveAll(tasks + indexSwitch).collectList().awaitFirst()
    }

    private suspend fun scheduleOrderReindex(indexName: String, definition: EntityDefinitionExtended) {
        val blockchains = activeBlockchainProvider.blockchains
        val taskParams = blockchains.map {
            paramFactory.toString(
                OrderTaskParam(
                    versionData = definition.versionData,
                    settingsHash = definition.settingsHash,
                    blockchain = it,
                    index = indexName
                )
            )
        }
        val tasks = tasks(EsOrder.ENTITY_DEFINITION.reindexTask, taskParams)
        val changeEsOrderAliasTaskParam = ChangeAliasTaskParam(
            indexName, taskParams
        )
        val indexSwitch = indexSwitchTask(
            entityName = EsEntity.ORDER.entityName,
            changeAliasTaskParam = changeEsOrderAliasTaskParam,
            definition
        )
        taskRepository.saveAll(tasks + indexSwitch).collectList().awaitFirst()
    }

    private suspend fun scheduleOwnershipReindex(indexName: String, definition: EntityDefinitionExtended) {
        val blockchains = activeBlockchainProvider.blockchains
        val taskParams = blockchains.flatMap { blockchain ->
            OwnershipTaskParam.Target.values().map { target ->
                paramFactory.toString(
                    OwnershipTaskParam(
                        versionData = definition.versionData,
                        settingsHash = definition.settingsHash,
                        blockchain = blockchain,
                        index = indexName,
                        target = target
                    )
                )
            }
        }
        val tasks = tasks(EsOwnership.ENTITY_DEFINITION.reindexTask, taskParams)
        val changeEsOwnershipAliasTaskParam = ChangeAliasTaskParam(
            indexName, taskParams
        )
        val indexSwitch = indexSwitchTask(
            entityName = EsEntity.OWNERSHIP.entityName,
            changeAliasTaskParam = changeEsOwnershipAliasTaskParam,
            definition
        )
        taskRepository.saveAll(tasks + indexSwitch).collectList().awaitFirst()
    }

    private suspend fun scheduleTraitReindex(indexName: String, definition: EntityDefinitionExtended) {
        val blockchains = activeBlockchainProvider.blockchains
        val taskParams = blockchains.map { blockchain ->
            paramFactory.toString(
                SyncTraitJobParam(
                    blockchain = blockchain,
                    scope = SyncScope.ES,
                    esIndex = indexName,
                )
            )
        }
        val tasks = tasks(EsTrait.ENTITY_DEFINITION.reindexTask, taskParams)
        val changeEsTraitAliasTaskParam = ChangeAliasTaskParam(
            indexName, taskParams
        )
        val indexSwitch = indexSwitchTask(
            entityName = EsEntity.TRAIT.entityName,
            changeAliasTaskParam = changeEsTraitAliasTaskParam,
            definition
        )
        taskRepository.saveAll(tasks + indexSwitch).collectList().awaitFirst()
    }

    private suspend fun tasks(reindexTask: String, params: List<String>): List<Task> =
        params.mapAsync {
            task(taskType = reindexTask, taskParamJson = it)
        }.filterNotNull()

    private suspend fun task(
        taskType: String,
        taskParamJson: String
    ): Task? {

        val existing = taskRepository
            .findByTypeAndParam(taskType, taskParamJson)
            .awaitSingleOrNull()
        return if (existing == null) {
            logger.info(
                "Scheduling {} reindexing with param: {}",
                taskType,
                taskParamJson
            )
            Task(
                type = taskType,
                param = taskParamJson,
                state = null,
                running = false,
                lastStatus = TaskStatus.NONE
            )
        } else {
            logger.info(
                "{} reindexing with param {} already exists with id={}",
                taskType, taskParamJson, existing.id.toHexString()
            )
            null
        }
    }

    private suspend fun indexSwitchTask(
        entityName: String,
        changeAliasTaskParam: ChangeAliasTaskParam,
        definition: EntityDefinitionExtended
    ): List<Task> {

        val taskParamJson = paramFactory.toString(changeAliasTaskParam)
        val taskType = definition.getChangeAliasTaskName()
        val existing = taskRepository
            .findByTypeAndParam(taskType, taskParamJson)
            .awaitSingleOrNull()
        return if (existing == null) {
            logger.info(
                "Scheduling {} index alias switch with param: {}",
                entityName,
                changeAliasTaskParam
            )
            listOf(
                Task(
                    type = taskType,
                    param = taskParamJson,
                    state = null,
                    running = false,
                    lastStatus = TaskStatus.NONE
                )
            )
        } else {
            logger.info(
                "{} index alias switch with param {} already exists with id={}",
                entityName, changeAliasTaskParam, existing.id.toHexString()
            )
            emptyList()
        }
    }

    override suspend fun stopTasksIfExists(entityDefinition: EntityDefinitionExtended) {
        val tasks = taskRepository.findAll()
            .filter { entityDefinition.reindexTask == it.type || entityDefinition.getChangeAliasTaskName() == it.type }
            .collectList()
            .awaitFirst()

        taskRepository.deleteAll(tasks).awaitFirstOrNull()
    }

    override suspend fun checkReindexInProgress(
        entityDefinition: EntityDefinitionExtended
    ): Boolean {
        val tasks = taskRepository.findAll()
            .filter {
                if (entityDefinition.reindexTask == it.type &&
                    it.lastStatus != TaskStatus.COMPLETED && it.lastStatus != TaskStatus.ERROR
                ) {
                    val taskParam = paramFactory.parse<RawTaskParam>(it.param)
                    entityDefinition.versionData == taskParam.versionData &&
                        entityDefinition.settingsHash == taskParam.settingsHash
                } else false
            }
            .collectList()
            .awaitFirst()

        return tasks.isNotEmpty()
    }

    companion object {
        val logger by Logger()
    }
}
