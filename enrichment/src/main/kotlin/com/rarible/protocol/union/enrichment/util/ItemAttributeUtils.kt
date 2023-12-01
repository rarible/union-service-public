package com.rarible.protocol.union.enrichment.util

import com.rarible.protocol.union.core.model.UnionMetaAttribute
import com.rarible.protocol.union.enrichment.model.ItemAttributeShort

fun UnionMetaAttribute.toItemAttributeShort(): ItemAttributeShort? {
    val value = this.value
    return if (value == null) null else ItemAttributeShort(key, value)
}
