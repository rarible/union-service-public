package com.rarible.protocol.union.api.controller

import com.rarible.core.logging.withTraceId
import com.rarible.protocol.union.api.service.select.CollectionSourceSelectService
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.model.TokenId
import com.rarible.protocol.union.core.service.CollectionService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionsDto
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.enrichment.configuration.UnionMetaProperties
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaPipeline
import com.rarible.protocol.union.enrichment.meta.collection.CollectionMetaRefreshService
import com.rarible.protocol.union.enrichment.model.MetaRefreshRequest
import com.rarible.protocol.union.enrichment.model.EnrichmentCollectionId
import com.rarible.protocol.union.enrichment.repository.MetaRefreshRequestRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentCollectionService
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@ExperimentalCoroutinesApi
@RestController
class CollectionController(
    private val router: BlockchainRouter<CollectionService>,
    private val collectionSourceSelector: CollectionSourceSelectService,
    private val enrichmentCollectionService: EnrichmentCollectionService,
    private val collectionMetaRefreshService: CollectionMetaRefreshService,
    private val metaRefreshRequestRepository: MetaRefreshRequestRepository,
    private val unionMetaProperties: UnionMetaProperties,
    private val ff: FeatureFlagsProperties
) : CollectionControllerApi {

    private val logger = LoggerFactory.getLogger(javaClass)

    override suspend fun getAllCollections(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<CollectionsDto> {
        val result = collectionSourceSelector.getAllCollections(blockchains, continuation, size)
        return ResponseEntity.ok(result)
    }

    override suspend fun getCollectionById(
        collection: String
    ): ResponseEntity<CollectionDto> {
        val fullCollectionId = IdParser.parseCollectionId(collection)
        val enrichmentCollectionId = EnrichmentCollectionId(fullCollectionId)
        val enrichmentCollection = enrichmentCollectionService.get(enrichmentCollectionId)

        val unionCollection = if (ff.enableUnionCollections) {
            if (enrichmentCollection == null) {
                throw UnionNotFoundException("Collection [$collection] not found")
            }
            null
        } else {
            router.getService(fullCollectionId.blockchain).getCollectionById(fullCollectionId.value)
        }

        val enrichedCollection = enrichmentCollectionService.enrichCollection(
            enrichmentCollection,
            unionCollection,
            emptyMap(),
            CollectionMetaPipeline.API
        )
        return ResponseEntity.ok(enrichedCollection)
    }

    override suspend fun refreshCollectionMeta(collection: String): ResponseEntity<Unit> = withTraceId {
        logger.info("Received request to refresh meta for collection: $collection")
        val collectionId = IdParser.parseCollectionId(collection)
        if (collectionMetaRefreshService.shouldRefresh(collectionId)) {
            metaRefreshRequestRepository.save(
                MetaRefreshRequest(
                    collectionId = collection,
                    full = true,
                    withSimpleHash = unionMetaProperties.simpleHash.enabled,
                )
            )
        }
        ResponseEntity.noContent().build()
    }

    override suspend fun getCollectionsByOwner(
        owner: String,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): ResponseEntity<CollectionsDto> {
        val ownerAddress = IdParser.parseAddress(owner)
        val result = collectionSourceSelector.getCollectionsByOwner(ownerAddress, blockchains, continuation, size)
        return ResponseEntity.ok(result)
    }

    @GetMapping(value = ["/v0.1/collections/{collectionId}/generate_token_id"])
    suspend fun generateId(
        @PathVariable("collectionId") collectionId: String,
        @RequestParam(value = "minter", required = false) minter: String,
    ): ResponseEntity<TokenId> {
        val fullCollectionId = IdParser.parseCollectionId(collectionId)
        val tokenId = router.getService(fullCollectionId.blockchain).generateNftTokenId(fullCollectionId.value, minter)
        return ResponseEntity.ok(tokenId)
    }
}
