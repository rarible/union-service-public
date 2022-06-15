package com.rarible.protocol.union.enrichment.service.query.collection

import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionsDto

interface CollectionQueryService {

    suspend fun getAllCollections(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): CollectionsDto

    suspend fun getCollectionsByOwner(
        owner: String,
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int?
    ): CollectionsDto
}
