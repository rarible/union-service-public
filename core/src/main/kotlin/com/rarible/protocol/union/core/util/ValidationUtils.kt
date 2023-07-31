package com.rarible.protocol.union.core.util

import com.rarible.protocol.union.core.exception.UnionValidationException

fun <T> checkNullIds(ids: List<T>) {
    ids.forEach { if (it == null) throw UnionValidationException("ID can't be null") }
}
