package com.rarible.protocol.union.core.model

import com.rarible.protocol.union.dto.UnionAddress

data class UnionCollectionMeta(
    val name: String,
    val description: String? = null,
    val content: List<UnionMetaContent> = listOf(),
    val externalLink: String? = null,
    val feeRecipient: UnionAddress? = null,
    val sellerFeeBasisPoints: Int? = null
)