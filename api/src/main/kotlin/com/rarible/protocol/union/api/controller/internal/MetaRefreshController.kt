package com.rarible.protocol.union.api.controller.internal

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.common.nowMillis
import com.rarible.core.logging.withTraceId
import com.rarible.core.task.TaskRepository
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.configuration.UnionMetaProperties
import com.rarible.protocol.union.enrichment.model.CollectionMetaRefreshRequest
import com.rarible.protocol.union.enrichment.repository.CollectionMetaRefreshRequestRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class MetaRefreshController(
    private val collectionMetaRefreshRequestRepository: CollectionMetaRefreshRequestRepository,
    private val unionMetaProperties: UnionMetaProperties,
    private val taskRepository: TaskRepository,
    private val objectMapper: ObjectMapper,
) {

    @PostMapping(
        value = ["/maintenance/meta/collections/refresh/full"],
        produces = ["application/json"]
    )
    suspend fun fullRefresh(
        @RequestParam(required = false) scheduledAt: Instant?,
        @RequestBody body: String,
        @RequestParam(value = "withSimpleHash", required = false, defaultValue = "false") withSimpleHash: Boolean
    ): Unit = withTraceId {
        scheduleRefresh(
            collections = parseCollectionsFromBody(body),
            full = true,
            scheduledAt = scheduledAt ?: nowMillis(),
            withSimpleHash = withSimpleHash && unionMetaProperties.simpleHash.enabled
        )
    }

    @PostMapping(
        value = ["/maintenance/meta/collections/refresh/emptyOnly"],
        produces = ["application/json"]
    )
    suspend fun emptyOnlyRefresh(
        @RequestParam(required = false) scheduledAt: Instant?,
        @RequestBody body: String
    ): Unit = withTraceId {
        scheduleRefresh(
            collections = parseCollectionsFromBody(body),
            full = false,
            scheduledAt = scheduledAt ?: nowMillis()
        )
    }

    @DeleteMapping(
        value = ["/maintenance/meta/collections/refresh/cancel"],
        produces = ["application/json"]
    )
    suspend fun cancelRefresh(): Unit = withTraceId {
        logger.info("Cancelling refresh")
        collectionMetaRefreshRequestRepository.deleteAll()
    }

    @GetMapping(value = ["/maintenance/meta/collections/refresh/status"], produces = ["text/plain"])
    suspend fun status(): String {
        val queueSize = collectionMetaRefreshRequestRepository.countNotScheduled()
        val processing = taskRepository.findByRunning(true)
            .filter { it.type in setOf("META_REFRESH_TASK", "REFRESH_SIMPLEHASH_TASK") }
            .asFlow().toList()
            .joinToString(", ") { objectMapper.readValue(it.param, Map::class.java)["collectionId"].toString() }
        return """Queue size: $queueSize
Currently processing: $processing""".trimIndent()
    }

    private fun parseCollectionsFromBody(body: String): List<String> {
        return body.split("\n").filter { it.isNotBlank() }.map { sanitise(it) }
    }

    private fun sanitise(value: String): String {
        val sanitised = value.replace("\"", "").trim()
        return IdParser.parseCollectionId(sanitised).fullId()
    }

    private suspend fun scheduleRefresh(
        collections: List<String>,
        full: Boolean,
        scheduledAt: Instant,
        withSimpleHash: Boolean = false
    ) {
        collections.map { id ->
            try {
                if (collectionMetaRefreshRequestRepository.countNotScheduledForCollectionId(id) == 0L) {
                    logger.info("Scheduling refresh for $id, full=$full at $scheduledAt")
                    collectionMetaRefreshRequestRepository.save(
                        CollectionMetaRefreshRequest(
                            collectionId = id,
                            full = full,
                            scheduledAt = scheduledAt,
                            withSimpleHash = withSimpleHash
                        )
                    )
                }
            } catch (e: Exception) {
                logger.error("Failed to schedule refresh for $id", e)
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MetaRefreshController::class.java)
    }
}