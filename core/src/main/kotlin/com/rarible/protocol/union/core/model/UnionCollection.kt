package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.UnionAddress

data class UnionCollection(
    val id: CollectionIdDto,
    val name: String,
    val status: CollectionDto.Status? = null,
    val type: CollectionDto.Type,
    val minters: List<UnionAddress>? = listOf(),
    val features: List<CollectionDto.Features> = listOf(),
    // TODO remove later
    val meta: UnionCollectionMeta? = null,
    val owner: UnionAddress? = null,
    val parent: CollectionIdDto? = null,
    val symbol: String? = null,
    val self: Boolean? = null,
)