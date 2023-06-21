package com.rarible.protocol.union.worker.task.meta

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.logging.withTraceId
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.model.MetaRefreshRequest
import com.rarible.protocol.union.enrichment.service.SimpleHashService
import com.rarible.protocol.union.worker.job.meta.MetaRefreshSchedulingService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import kotlin.time.Duration.Companion.seconds

@Component
@ConditionalOnProperty("meta.simpleHash.enabled", havingValue = "true")
class RefreshMetaSimpleHashTask(
    private val objectMapper: ObjectMapper,
    private val simpleHashService: SimpleHashService,
    private val metaRefreshSchedulingService: MetaRefreshSchedulingService,
) : TaskHandler<Unit> {
    override val type = META_REFRESH_SIMPLEHASH_TASK

    override fun runLongTask(from: Unit?, param: String): Flow<Unit> = flow<Unit> {
        logger.info("Starting RefreshSimpleHashTask from=$from, param=$param")
        val parsedParam = objectMapper.readValue(param, RefreshSimpleHashTaskParam::class.java)
        simpleHashService.refreshContract(IdParser.parseCollectionId(parsedParam.collectionId))
        delay(30.seconds)
        metaRefreshSchedulingService.scheduleTask(
            MetaRefreshRequest(
                collectionId = parsedParam.collectionId,
                full = true,
            )
        )
        logger.info("Finished RefreshSimpleHashTask from=$from, param=$param")
    }.withTraceId()

    companion object {
        const val META_REFRESH_SIMPLEHASH_TASK = "META_REFRESH_SIMPLEHASH_TASK"
        private val logger = LoggerFactory.getLogger(RefreshMetaSimpleHashTask::class.java)
    }
}

data class RefreshSimpleHashTaskParam(
    val collectionId: String
)
