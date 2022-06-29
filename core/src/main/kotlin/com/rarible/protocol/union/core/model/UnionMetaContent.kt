package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.MetaContentDto

data class UnionMetaContent(
    val url: String,
    val representation: MetaContentDto.Representation,
    val fileName: String? = null,
    val properties: UnionMetaContentProperties? = null
)
