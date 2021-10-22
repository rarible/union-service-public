package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.MetaAttributeDto

data class UnionMeta(
    val name: String,
    val description: String? = null,
    val attributes: List<MetaAttributeDto>,
    val content: List<UnionMetaContent>
)
