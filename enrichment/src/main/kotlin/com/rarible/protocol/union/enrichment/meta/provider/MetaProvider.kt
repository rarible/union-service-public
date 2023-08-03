package com.rarible.protocol.union.enrichment.meta.provider

import com.rarible.protocol.union.core.model.ContentOwner
import com.rarible.protocol.union.core.model.download.MetaProviderType

interface MetaProvider<K, T : ContentOwner<T>> {

    fun getType(): MetaProviderType

    suspend fun fetch(key: K, original: T?): T?
}
