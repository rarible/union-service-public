package com.rarible.protocol.union.api.controller.internal

import com.rarible.protocol.union.api.model.internal.EsMaintenanceReindexActivityDto
import com.rarible.protocol.union.api.model.internal.EsMaintenanceReindexOwnershipDto
import com.rarible.protocol.union.api.service.elastic.ElasticMaintenanceService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class EsMaintenanceController(
    private val elasticMaintenanceService: ElasticMaintenanceService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)


    @PostMapping(
        value = ["/maintenance/es/reindex/activity"],
        consumes = ["application/json"]
    )
    suspend fun reindexActivities(@RequestBody payload: EsMaintenanceReindexActivityDto) {
        logger.info("Got request to reindex activities: $payload")
        elasticMaintenanceService.scheduleReindexActivitiesTasks(
            blockchains = payload.blockchains,
            types = payload.types,
            from = payload.from,
            to = payload.to,
            esIndex = payload.esIndex,
        )
        logger.info("Reindex activities tasks scheduled: $payload")
    }

    @PostMapping(
        value = ["/maintenance/es/reindex/ownership"],
        consumes = ["application/json"]
    )
    suspend fun reindexOwnerships(@RequestBody payload: EsMaintenanceReindexOwnershipDto) {
        logger.info("Got request to reindex ownerships: $payload")
        elasticMaintenanceService.scheduleReindexOwnershipsTasks(
            blockchains = payload.blockchains,
            from = payload.from,
            to = payload.to,
            esIndex = payload.esIndex,
        )
        logger.info("Reindex ownerships tasks scheduled: $payload")
    }
}
