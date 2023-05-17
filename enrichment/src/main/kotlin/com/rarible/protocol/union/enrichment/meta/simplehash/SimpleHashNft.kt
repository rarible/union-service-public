package com.rarible.protocol.union.enrichment.meta.simplehash

import java.time.LocalDateTime


data class SimpleHashNft(
    val tokenId: String,
    val name: String?,
    val description: String?,
    val previews: Preview?,
    val extraMetadata: ExtraMetadata?,
    val collection: Collection?,
    val createdDate: LocalDateTime?,
    val externalUrl: String?,
    val metadataOriginalUrl: String?,
) {
    data class Preview(
        val imageSmallUrl: String?,
        val imageMediumUrl: String?,
        val imageLargeUrl: String?,
        val imageOpengraphUrl: String?
    )

    data class ExtraMetadata(
        val imageOriginalUrl: String?,
        val attributes: List<Attribute> = emptyList()
    )

    data class Collection(
        val name: String?,
        val description: String?
    )

    data class Attribute(
        val traitType: String,
        val value: String?,
        val displayType: String?
    )
}
