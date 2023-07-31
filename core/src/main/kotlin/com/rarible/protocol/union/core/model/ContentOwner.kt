package com.rarible.protocol.union.core.model

interface ContentOwner<T : ContentOwner<T>> {

    val content: List<UnionMetaContent>

    fun withContent(content: List<UnionMetaContent>): T
}
