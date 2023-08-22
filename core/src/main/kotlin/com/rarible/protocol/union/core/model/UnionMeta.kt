package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.core.model.download.MetaSource
import java.time.Instant

data class UnionMeta(
    val name: String,
    val source: MetaSource?,
    val contributors: List<MetaSource> = emptyList(),
    val collectionId: String? = null,
    val description: String? = null,
    val createdAt: Instant? = null,
    val tags: List<String> = emptyList(),
    val genres: List<String> = emptyList(),
    val language: String? = null,
    val rights: String? = null,

    val rightsUri: String? = null,
    val externalUri: String? = null,
    val originalMetaUri: String? = null,

    val attributes: List<UnionMetaAttribute> = emptyList(),
    override val content: List<UnionMetaContent> = emptyList(),

    @Deprecated("Not supported, should be removed")
    val restrictions: List<Restriction> = emptyList()
) : ContentOwner<UnionMeta> {

    override fun withContent(content: List<UnionMetaContent>) = this.copy(content = content)

    fun toComparable(): UnionMeta = copy(
        createdAt = null,
        source = null,
        contributors = emptyList(),
        // Goal is to check only url and representation
        content = content.map { it.copy(properties = null, fileName = null) }
    )
}
