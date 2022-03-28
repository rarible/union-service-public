package com.rarible.protocol.union.core.domain

data class ItemMeta(
    val name: String,
    val description: String? = null,
    val image: ItemMedia? = null,
    val animation: ItemMedia? = null,
    val origMedias: List<ItemMedia> = emptyList(),
    val attributes: List<ItemAttribute> = emptyList()
) {
    companion object {
        const val MAX_NAME_LENGTH = 1024
        const val MAX_DESCRIPTION_LENGTH = 10000
    }
}

data class ItemMedia(
    val url: String,
    val meta: MediaMeta? = null
)
