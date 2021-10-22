package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.MetaContentDto

data class UnionMetaContent(
    val url: String,
    val representation: MetaContentDto.Representation,
    // Metadata from blockchain - in most cases it is null, but if we have it, we can use it instead of
    // downloading data in union-service
    val properties: UnionMetaContentProperties? = null
)