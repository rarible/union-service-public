package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.MetaAttributeDto
import java.time.Instant

data class UnionMeta(
    val name: String,
    val description: String? = null,
    val createdAt: Instant? = null,
    val tags: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val language: String? = null,
    val rights: String? = null,

    val rightsUri: String? = null,
    val externalUri: String? = null,
    val originalMetaUri: String? = null,

    val attributes: List<MetaAttributeDto> = emptyList(),
    val content: List<UnionMetaContent> = emptyList(),

    @Deprecated("Not supported, should be removed")
    val restrictions: List<Restriction> = emptyList()
)