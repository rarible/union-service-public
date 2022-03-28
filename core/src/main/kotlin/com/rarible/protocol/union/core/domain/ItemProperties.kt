package com.rarible.protocol.union.core.domain

data class ItemProperties(
    val name: String,
    val description: String?,
    val attributes: List<ItemAttribute>,
    val mediaEntries: List<Media>? = null
)

data class ItemAttribute(
    val key: String,
    val value: String?,
    val type: String? = null,
    val format: String? = null
)
