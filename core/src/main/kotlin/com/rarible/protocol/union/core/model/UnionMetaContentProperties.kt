package com.rarible.protocol.union.core.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY)
@JsonSubTypes(
    JsonSubTypes.Type(name = "IMAGE", value = UnionImageProperties::class),
    JsonSubTypes.Type(name = "VIDEO", value = UnionVideoProperties::class),
    JsonSubTypes.Type(name = "AUDIO", value = UnionAudioProperties::class),
    JsonSubTypes.Type(name = "MODEL_3D", value = UnionModel3dProperties::class)
)
sealed class UnionMetaContentProperties {

    abstract val mimeType: String?
    abstract val size: Long?

    abstract fun isEmpty(): Boolean
}

data class UnionImageProperties(
    override val mimeType: String? = null,
    override val size: Long? = null,
    val width: Int? = null,
    val height: Int? = null
) : UnionMetaContentProperties() {

    override fun isEmpty(): Boolean = mimeType == null || width == null || height == null

}

data class UnionVideoProperties(
    override val mimeType: String? = null,
    override val size: Long? = null,
    val width: Int? = null,
    val height: Int? = null
) : UnionMetaContentProperties() {

    override fun isEmpty(): Boolean = mimeType == null || width == null || height == null

}

data class UnionAudioProperties(
    override val mimeType: String? = null,
    override val size: Long? = null,
    val duration: Int? = null
) : UnionMetaContentProperties() {

    override fun isEmpty(): Boolean = mimeType == null // Do not consider duration, we can't extract it ATM

}

data class UnionModel3dProperties(
    override val mimeType: String? = null,
    override val size: Long? = null
) : UnionMetaContentProperties() {

    override fun isEmpty(): Boolean = mimeType == null

}