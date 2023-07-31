package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.UnionAddress
import java.time.Instant

data class UnionCollectionMeta(
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

    val feeRecipient: UnionAddress? = null,
    val sellerFeeBasisPoints: Int? = null,

    override val content: List<UnionMetaContent> = listOf()
) : ContentOwner<UnionCollectionMeta> {

    override fun withContent(content: List<UnionMetaContent>) = this.copy(content = content)
}
