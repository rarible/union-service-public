package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.UnionAddress

data class UnionCollection(
    val id: CollectionIdDto,
    val name: String,
    val type: CollectionDto.Type,
    val minters: List<UnionAddress>? = listOf(),
    val features: List<CollectionDto.Features> = listOf(),
    val meta: UnionCollectionMeta? = null,
    val owner: UnionAddress? = null,
    val parent: CollectionIdDto? = null,
    val symbol: String? = null,
    val self: Boolean? = null,
)