package com.rarible.protocol.union.core.util

import com.rarible.protocol.union.dto.continuation.page.Page
import com.rarible.protocol.union.dto.continuation.page.Slice

fun <T> Page<T>.toSlice() = Slice(this.continuation, this.entities)