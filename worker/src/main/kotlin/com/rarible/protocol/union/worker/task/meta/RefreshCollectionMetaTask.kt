package com.rarible.protocol.union.worker.task.meta

import com.fasterxml.jackson.databind.ObjectMapper
import com.rarible.core.logging.withTraceId
import com.rarible.core.task.TaskHandler
import com.rarible.protocol.union.core.task.RefreshCollectionMetaTaskParam
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.download.DownloadTaskSource
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaPipeline
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaService
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class RefreshCollectionMetaTask(
    private val collectionRepository: CollectionRepository,
    private val objectMapper: ObjectMapper,
    private val collectionMetaService: CollectionMetaService,
) : TaskHandler<String> {
    override val type = RefreshCollectionMetaTaskParam.COLLECTION_META_REFRESH_TASK

    override fun runLongTask(from: String?, param: String): Flow<String> = flow {
        logger.info("Starting RefreshCollectionMetaTask from=$from, param=$param")
        val parsedParam = objectMapper.readValue(param, RefreshCollectionMetaTaskParam::class.java)
        val collectionId = from?.let { EnrichmentCollectionId(IdParser.parseCollectionId(it)) }
        collectionRepository.findAll(fromIdExcluded = collectionId, blockchain = parsedParam.blockchain)
            .collect { collection ->
                if (parsedParam.full || collection.metaEntry == null) {
                    collectionMetaService.schedule(
                        collectionId = collection.id.toDto(),
                        pipeline = CollectionMetaPipeline.REFRESH,
                        force = true,
                        source = DownloadTaskSource.INTERNAL,
                        priority = parsedParam.priority,
                    )
                }
                emit(collection.id.toString())
            }
        logger.info("Finished RefreshCollectionMetaTask from=$from, param=$param")
    }.withTraceId()

    companion object {
        private val logger = LoggerFactory.getLogger(RefreshCollectionMetaTask::class.java)
    }
}
