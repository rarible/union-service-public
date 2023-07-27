package com.rarible.protocol.union.enrichment.custom.collection

import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.ItemIdDto

data class CustomCollectionResolutionRequest<T>(
    val entityId: T,
    val itemId: ItemIdDto?,
    val collectionId: CollectionIdDto?
)
