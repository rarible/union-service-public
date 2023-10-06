package com.rarible.protocol.union.enrichment.meta.simplehash

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import java.time.LocalDateTime

/**
 * https://docs.simplehash.com/reference/nft-model
 */
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
@JsonIgnoreProperties(ignoreUnknown = true)
data class SimpleHashItem(
    val nftId: String,
    val tokenId: String?,
    val name: String?,
    val description: String?,
    val previews: Preview?,
    val imageProperties: ImageProperties?,
    val extraMetadata: ExtraMetadata?,
    val collection: Collection?,
    val createdDate: LocalDateTime?,
    val externalUrl: String?,
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Preview(
        val imageSmallUrl: String?,
        val imageMediumUrl: String?,
        val imageLargeUrl: String?,
        val imageOpengraphUrl: String?,
    )

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ExtraMetadata(
        val imageOriginalUrl: String?,
        val attributes: List<Attribute> = emptyList(),
        val features: Map<String, String>? = null,
        val projectId: String? = null,
        val collectionName: String? = null,
        val metadataOriginalUrl: String? = null,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Collection(
        val name: String?,
        val description: String?,
        val imageUrl: String?,
        val bannerImageUrl: String?,
    )

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Attribute(
        val traitType: String?,
        val value: String?,
        val displayType: String?
    )

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy::class)
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class ImageProperties(
        val width: Int?,
        val height: Int?,
        val size: Long?,
        val mimeType: String?
    )

    fun differentOriginalUrls(another: SimpleHashItem): Boolean {
        return extraMetadata?.metadataOriginalUrl != another.extraMetadata?.metadataOriginalUrl ||
                extraMetadata?.imageOriginalUrl != another.extraMetadata?.imageOriginalUrl
    }

    fun hasOriginalsUrls(): Boolean {
        return extraMetadata?.metadataOriginalUrl != null || extraMetadata?.imageOriginalUrl != null
    }
}
