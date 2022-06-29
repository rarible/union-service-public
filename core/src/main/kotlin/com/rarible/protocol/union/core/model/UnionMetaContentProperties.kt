package com.rarible.protocol.union.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "IMAGE", value = UnionImageProperties::class),
    JsonSubTypes.Type(name = "VIDEO", value = UnionVideoProperties::class),
    JsonSubTypes.Type(name = "AUDIO", value = UnionAudioProperties::class),
    JsonSubTypes.Type(name = "MODEL_3D", value = UnionModel3dProperties::class),
    JsonSubTypes.Type(name = "HTML", value = UnionHtmlProperties::class),
    JsonSubTypes.Type(name = "UNKNOWN", value = UnionUnknownProperties::class)
)
sealed class UnionMetaContentProperties {

    abstract val mimeType: String?
    abstract val size: Long?

    abstract fun isEmpty(): Boolean

    abstract fun isFull(): Boolean
}

data class UnionImageProperties(
    override val mimeType: String? = null,
    override val size: Long? = null,
    val width: Int? = null,
    val height: Int? = null
) : UnionMetaContentProperties() {

    override fun isEmpty(): Boolean = mimeType == null || width == null || height == null

    override fun isFull(): Boolean {
        return mimeType != null
            && size != null
            && width != null
            && height != null
    }

}

data class UnionVideoProperties(
    override val mimeType: String? = null,
    override val size: Long? = null,
    val width: Int? = null,
    val height: Int? = null
) : UnionMetaContentProperties() {

    override fun isEmpty(): Boolean = mimeType == null || width == null || height == null

    override fun isFull(): Boolean {
        return mimeType != null
            && size != null
            && width != null
            && height != null
    }
}

data class UnionAudioProperties(
    override val mimeType: String? = null,
    override val size: Long? = null
) : UnionMetaContentProperties() {

    override fun isEmpty(): Boolean = mimeType == null

    override fun isFull(): Boolean = mimeType != null && size != null

}

data class UnionModel3dProperties(
    override val mimeType: String? = null,
    override val size: Long? = null
) : UnionMetaContentProperties() {

    override fun isEmpty(): Boolean = mimeType == null

    override fun isFull(): Boolean = mimeType != null && size != null

}

data class UnionHtmlProperties(
    override val mimeType: String? = null,
    override val size: Long? = null
) : UnionMetaContentProperties() {

    override fun isEmpty(): Boolean = mimeType == null

    override fun isFull(): Boolean = mimeType != null && size != null

}

data class UnionUnknownProperties(
    override val mimeType: String? = null,
    override val size: Long? = null
) : UnionMetaContentProperties() {

    override fun isEmpty(): Boolean = mimeType == null

    override fun isFull(): Boolean = false

}