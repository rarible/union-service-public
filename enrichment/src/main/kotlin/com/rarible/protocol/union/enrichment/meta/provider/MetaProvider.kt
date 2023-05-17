package com.rarible.protocol.union.enrichment.meta.provider

import com.rarible.protocol.union.core.model.ContentOwner
import com.rarible.protocol.union.enrichment.meta.WrappedMeta

interface MetaProvider<K, T : ContentOwner<T>> {

    suspend fun fetch(key: K): WrappedMeta<T>?

}
