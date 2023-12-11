package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.CollectionIdDto

data class UnionTraitEvent(
    val id: String,
    val collectionId: CollectionIdDto,
    val key: String,
    val value: String?,
    val itemsCount: Long,
    val listedItemsCount: Long,
    val version: Long,
)
