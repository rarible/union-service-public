package com.rarible.protocol.union.enrichment.meta.provider

import com.rarible.protocol.union.core.model.ContentOwner
import com.rarible.protocol.union.core.model.MetaSource
import com.rarible.protocol.union.dto.BlockchainDto

/**
 * "Global" metadata provider which is used by default to resolve meta for ALL entities.
 * If there are several "global" providers, they could extend metadata, received by previous provider
 */
interface MetaProvider<T : ContentOwner<T>> {

    fun getSource(): MetaSource

    suspend fun fetch(blockchain: BlockchainDto, id: String, original: T?): T?
}
