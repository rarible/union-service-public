package com.rarible.protocol.union.core.model

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