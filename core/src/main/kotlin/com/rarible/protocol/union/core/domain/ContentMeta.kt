package com.rarible.protocol.union.core.domain

data class ContentMeta(
    val imageMeta: MediaMeta?,
    val animationMeta: MediaMeta?
)

data class MediaMeta(
    val type: String?,
    val width: Int? = null,
    val height: Int? = null
) {
    fun toDimension(): Dimension? {
        return if (width != null && height != null) {
            Dimension(width = width, height = height)
        } else {
            null
        }
    }
}

enum class SizeType {
    SOURCE,
    EXTRA_SOURCE,
    ORIGINAL,
    PREVIEW,
    BIG,
    PLACEHOLDER,
    MOBILE_LOW,
    MOBILE_MEDIUM,
    MOBILE_HIGH
}

enum class ContentType(val cloudinaryType: String) {
    IMAGE("image"),
    VIDEO("video"),
    AUDIO("audio"),
    THREE_D("image"),
    AR("image");
}

enum class TransformationType {
    IMAGE,
    VIDEO,
    AUDIO,
    THREE_D,
    AR,

    GIF_TO_VIDEO {
        override fun getContentType(contentType: ContentType): ContentType = ContentType.VIDEO
    },
    GIF_TO_THUMBNAIL {
        override fun getContentType(contentType: ContentType): ContentType = ContentType.IMAGE
    },

    VIDEO_TO_THUMBNAIL {
        override fun getContentType(contentType: ContentType): ContentType = ContentType.IMAGE
    },

    AVATAR,
    COVER;

    open fun getContentType(contentType: ContentType): ContentType = contentType
}

val SUPPORTED_SIZE_TYPES = mapOf(
    TransformationType.IMAGE to listOf(
        SizeType.ORIGINAL, SizeType.PREVIEW, SizeType.BIG,
        SizeType.MOBILE_LOW, SizeType.MOBILE_MEDIUM, SizeType.MOBILE_HIGH
    ),
    TransformationType.VIDEO to listOf(
        SizeType.ORIGINAL, SizeType.PREVIEW, SizeType.BIG,
        SizeType.MOBILE_LOW, SizeType.MOBILE_HIGH
    ),
    TransformationType.AUDIO to listOf(
        SizeType.ORIGINAL
    ),
    TransformationType.THREE_D to listOf(
        SizeType.ORIGINAL
    ),
    TransformationType.AR to listOf(
        SizeType.ORIGINAL
    ),
    TransformationType.GIF_TO_VIDEO to listOf(
        SizeType.ORIGINAL, SizeType.PREVIEW, SizeType.BIG,
        SizeType.MOBILE_LOW, SizeType.MOBILE_HIGH
    ),
    TransformationType.GIF_TO_THUMBNAIL to listOf(
        SizeType.ORIGINAL, SizeType.PREVIEW, SizeType.BIG
    ),
    TransformationType.VIDEO_TO_THUMBNAIL to listOf(
        SizeType.ORIGINAL, SizeType.PREVIEW, SizeType.BIG, SizeType.MOBILE_LOW, SizeType.MOBILE_MEDIUM,
        SizeType.MOBILE_HIGH
    ),
    TransformationType.AVATAR to listOf(
        SizeType.ORIGINAL, SizeType.PREVIEW, SizeType.BIG
    ),
    TransformationType.COVER to listOf(
        SizeType.ORIGINAL, SizeType.PREVIEW, SizeType.BIG,
        SizeType.PLACEHOLDER
    )
)

val SUPPORTED_DIMENSIONS = mapOf(
    Pair(TransformationType.IMAGE, SizeType.PREVIEW) to Dimension(400, 400),
    Pair(TransformationType.IMAGE, SizeType.BIG) to Dimension(1000, 1000),
    Pair(TransformationType.IMAGE, SizeType.MOBILE_LOW) to Dimension(375, 0),
    Pair(TransformationType.IMAGE, SizeType.MOBILE_MEDIUM) to Dimension(750, 0),
    Pair(TransformationType.IMAGE, SizeType.MOBILE_HIGH) to Dimension(1125, 0),
    Pair(TransformationType.VIDEO, SizeType.PREVIEW) to Dimension(400, 400),
    Pair(TransformationType.VIDEO, SizeType.BIG) to Dimension(1000, 1000),
    Pair(TransformationType.VIDEO, SizeType.MOBILE_LOW) to Dimension(375, 0),
    Pair(TransformationType.VIDEO, SizeType.MOBILE_HIGH) to Dimension(1125, 0),
    Pair(TransformationType.GIF_TO_VIDEO, SizeType.PREVIEW) to Dimension(400, 400),
    Pair(TransformationType.GIF_TO_VIDEO, SizeType.BIG) to Dimension(1000, 1000),
    Pair(TransformationType.GIF_TO_VIDEO, SizeType.MOBILE_LOW) to Dimension(375, 0),
    Pair(TransformationType.GIF_TO_VIDEO, SizeType.MOBILE_HIGH) to Dimension(1125, 0),
    Pair(TransformationType.GIF_TO_THUMBNAIL, SizeType.PREVIEW) to Dimension(400, 400),
    Pair(TransformationType.GIF_TO_THUMBNAIL, SizeType.BIG) to Dimension(1000, 1000),
    Pair(TransformationType.VIDEO_TO_THUMBNAIL, SizeType.PREVIEW) to Dimension(400, 400),
    Pair(TransformationType.VIDEO_TO_THUMBNAIL, SizeType.BIG) to Dimension(1000, 1000),
    Pair(TransformationType.AVATAR, SizeType.PREVIEW) to Dimension(30, 30),
    Pair(TransformationType.AVATAR, SizeType.BIG) to Dimension(240, 240),
    Pair(TransformationType.COVER, SizeType.PREVIEW) to Dimension(1000, 200),
    Pair(TransformationType.COVER, SizeType.BIG) to Dimension(2200, 400),
    Pair(TransformationType.COVER, SizeType.PLACEHOLDER) to Dimension(55, 10)
)

enum class CropMode {
    FIT,
    THUMB,
    CROP
}

data class Dimension(
    val width: Int,
    val height: Int
)

data class Media(
    val url: String,
    val sizeType: SizeType,
    val contentType: ContentType,
    val dimension: Dimension? = null,
    val cropMode: CropMode? = null
)
