package com.rarible.protocol.union.api.controller.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.common.nowMillis
import com.rarible.core.logging.withTraceId
import com.rarible.core.task.TaskRepository
import com.rarible.protocol.union.api.service.task.TaskSchedulingService
import com.rarible.protocol.union.core.task.RefreshCollectionMetaTaskParam
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.configuration.CommonMetaProperties
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaRefreshService
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class MetaRefreshController(
    private val itemMetaRefreshService: ItemMetaRefreshService,
    private val commonMetaProperties: CommonMetaProperties,
    private val taskRepository: TaskRepository,
    private val objectMapper: ObjectMapper,
    private val taskSchedulingService: TaskSchedulingService,
) {

    @PostMapping(
        value = ["/maintenance/items/meta/refresh/{mode}"],
        produces = ["application/json"]
    )
    suspend fun fullRefresh(
        @PathVariable mode: String,
        @RequestParam(required = false) scheduledAt: Instant?,
        @RequestBody body: String,
        @RequestParam(value = "withSimpleHash", required = false, defaultValue = "false") withSimpleHash: Boolean
    ): Unit = withTraceId {
        itemMetaRefreshService.scheduleInternalRefresh(
            collections = parseCollectionsFromBody(body),
            full = "full" == mode,
            scheduledAt = scheduledAt ?: nowMillis(),
            withSimpleHash = withSimpleHash && commonMetaProperties.simpleHash.enabled
        )
    }

    @PostMapping(
        value = ["/maintenance/collections/meta/refresh/{mode}"],
        produces = ["application/json"]
    )
    suspend fun refreshCollectionMeta(
        @PathVariable mode: String,
        @RequestParam(value = "blockchain", required = false) blockchain: BlockchainDto? = null
    ): Unit = withTraceId {
        taskSchedulingService.schedule(
            type = RefreshCollectionMetaTaskParam.COLLECTION_META_REFRESH_TASK,
            param = objectMapper.writeValueAsString(
                RefreshCollectionMetaTaskParam(
                    blockchain = blockchain,
                    full = mode == "full",
                )
            )
        )
    }

    @DeleteMapping(
        value = ["/maintenance/items/meta/refresh/cancel"],
        produces = ["application/json"]
    )
    suspend fun cancelRefresh(): Unit = withTraceId {
        logger.info("Cancelling refresh")
        itemMetaRefreshService.deleteAllScheduledRequests()
    }

    @GetMapping(value = ["/maintenance/items/meta/refresh/status"], produces = ["text/plain"])
    suspend fun status(): String {
        val queueSize = itemMetaRefreshService.countNotScheduled()
        val processing = taskRepository.findByRunning(true)
            .filter { it.type in setOf("META_REFRESH_TASK", "META_REFRESH_SIMPLEHASH_TASK") }
            .asFlow().toList()
            .joinToString(", ") { objectMapper.readValue(it.param, Map::class.java)["collectionId"].toString() }
        return """Queue size: $queueSize
Currently processing: $processing""".trimIndent()
    }

    private fun parseCollectionsFromBody(body: String): List<CollectionIdDto> {
        return body.split("\n")
            .filter { it.isNotBlank() }
            .map { it.replace("\"", "").trim() }
            .map { IdParser.parseCollectionId(it) }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MetaRefreshController::class.java)
    }
}
