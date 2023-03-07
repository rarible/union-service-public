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
    abstract val available: Boolean?

    abstract fun isEmpty(): Boolean

    abstract fun isFull(): Boolean

    abstract fun withAvailable(available: Boolean): UnionMetaContentProperties
}

data class UnionImageProperties(
    override val mimeType: String? = null,
    override val size: Long? = null,
    override val available: Boolean? = null,
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

    override fun withAvailable(available: Boolean): UnionImageProperties = copy(available = available)
}

data class UnionVideoProperties(
    override val mimeType: String? = null,
    override val size: Long? = null,
    override val available: Boolean? = null,
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

    override fun withAvailable(available: Boolean): UnionVideoProperties = copy(available = available)
}

data class UnionAudioProperties(
    override val mimeType: String? = null,
    override val size: Long? = null,
    override val available: Boolean? = null,
) : UnionMetaContentProperties() {

    override fun isEmpty(): Boolean = mimeType == null

    override fun isFull(): Boolean = mimeType != null && size != null

    override fun withAvailable(available: Boolean): UnionAudioProperties = copy(available = available)
}

data class UnionModel3dProperties(
    override val mimeType: String? = null,
    override val size: Long? = null,
    override val available: Boolean? = null,
) : UnionMetaContentProperties() {

    override fun isEmpty(): Boolean = mimeType == null

    override fun isFull(): Boolean = mimeType != null && size != null

    override fun withAvailable(available: Boolean): UnionModel3dProperties = copy(available = available)
}

data class UnionHtmlProperties(
    override val mimeType: String? = null,
    override val size: Long? = null,
    override val available: Boolean? = null,
) : UnionMetaContentProperties() {

    override fun isEmpty(): Boolean = mimeType == null

    override fun isFull(): Boolean = mimeType != null && size != null

    override fun withAvailable(available: Boolean): UnionHtmlProperties = copy(available = available)
}

data class UnionUnknownProperties(
    override val mimeType: String? = null,
    override val size: Long? = null,
    override val available: Boolean? = null,
) : UnionMetaContentProperties() {

    override fun isEmpty(): Boolean = mimeType == null

    override fun isFull(): Boolean = false

    override fun withAvailable(available: Boolean): UnionUnknownProperties = copy(available = available)
}