package com.rarible.protocol.union.api.controller.internal

import com.rarible.core.logging.withTraceId
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.util.LogUtils
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.model.MetaAutoRefreshState
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.repository.MetaAutoRefreshStateRepository
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class MetaAutoRefreshController(
    private val collectionRepository: CollectionRepository,
    private val metaAutoRefreshStateRepository: MetaAutoRefreshStateRepository,
) {
    @PostMapping(
        value = ["/maintenance/items/meta/autoRefresh/{id}"],
        produces = ["application/json"]
    )
    suspend fun createAutoRefresh(
        @PathVariable id: String,
    ): Unit = withTraceId {
        val collectionId = IdParser.parseCollectionId(id)
        collectionRepository.get(EnrichmentCollectionId(collectionId))
            ?: throw UnionNotFoundException("Collection $id nod found")
        LogUtils.addToMdc(collectionId) {
            logger.info("Create meta auto refresh state for $collectionId by admin request")
        }
        metaAutoRefreshStateRepository.save(
            MetaAutoRefreshState(
                id = id,
            )
        )
    }

    @DeleteMapping(
        value = ["/maintenance/items/meta/autoRefresh/{id}"],
        produces = ["application/json"]
    )
    suspend fun deleteAutoRefresh(
        @PathVariable id: String,
    ): Unit = withTraceId {
        val collectionId = IdParser.parseCollectionId(id)
        LogUtils.addToMdc(collectionId) {
            logger.info("Delete meta auto refresh state for $collectionId by admin request")
        }
        metaAutoRefreshStateRepository.delete(id)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MetaAutoRefreshController::class.java)
    }
}
