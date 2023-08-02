package com.rarible.protocol.union.enrichment.meta

import com.rarible.protocol.union.core.model.ContentOwner

interface MetaCustomizer<K, T : ContentOwner<T>> {

    suspend fun customize(id: K, meta: T): T
}
