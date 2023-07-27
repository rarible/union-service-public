package com.rarible.protocol.union.enrichment.meta.provider

import com.rarible.protocol.union.core.model.ContentOwner
import com.rarible.protocol.union.core.model.download.MetaProviderType
import com.rarible.protocol.union.enrichment.meta.WrappedMeta

interface MetaProvider<K, T : ContentOwner<T>> {

    fun getType(): MetaProviderType

    suspend fun fetch(key: K, original: WrappedMeta<T>?): WrappedMeta<T>?
}
