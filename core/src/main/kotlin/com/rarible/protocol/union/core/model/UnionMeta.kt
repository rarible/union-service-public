package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.MetaAttributeDto
import com.rarible.protocol.union.dto.Restriction

data class UnionMeta(
    val name: String,
    val description: String? = null,
    val attributes: List<MetaAttributeDto>,
    val content: List<UnionMetaContent>,
    val restrictions: List<Restriction>
)
