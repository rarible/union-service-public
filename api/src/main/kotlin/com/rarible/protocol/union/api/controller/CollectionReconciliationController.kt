package com.rarible.protocol.union.api.controller

import com.rarible.protocol.union.core.exception.UnionException
import com.rarible.protocol.union.dto.CollectionsDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaPipeline
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.repository.CollectionRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentCollectionService
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Instant

@RestController
class CollectionReconciliationController(
    private val enrichmentCollectionService: EnrichmentCollectionService,
    private val collectionRepository: CollectionRepository,
) {

    @GetMapping(value = ["/reconciliation/collections"], produces = [MediaType.APPLICATION_JSON_VALUE])
    suspend fun getCollections(
        @RequestParam lastUpdatedFrom: Instant,
        @RequestParam lastUpdatedTo: Instant,
        @RequestParam(required = false) continuation: String? = null,
        @RequestParam(required = false, defaultValue = "20") size: Int,
    ): CollectionsDto {
        if (size !in 1..200) throw UnionException("Size param must be between 1 and 200")

        val enrichmentCollections = collectionRepository.findIdsByLastUpdatedAt(
            lastUpdatedFrom = lastUpdatedFrom,
            lastUpdatedTo = lastUpdatedTo,
            continuation = continuation?.let { EnrichmentCollectionId(IdParser.parseCollectionId(continuation)) },
            size = size
        )
        if (enrichmentCollections.isEmpty()) {
            return CollectionsDto()
        }

        return CollectionsDto(
            total = 0,
            collections = enrichmentCollectionService.enrich(enrichmentCollections, CollectionMetaPipeline.API),
            continuation = enrichmentCollections.last().id.toDto().fullId()
        )
    }
}
