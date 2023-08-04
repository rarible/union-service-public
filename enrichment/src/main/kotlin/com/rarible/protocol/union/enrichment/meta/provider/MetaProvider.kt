package com.rarible.protocol.union.enrichment.meta.provider

import com.rarible.protocol.union.core.model.ContentOwner
import com.rarible.protocol.union.core.model.download.MetaSource
import com.rarible.protocol.union.dto.BlockchainDto

interface MetaProvider<T : ContentOwner<T>> {

    fun getSource(): MetaSource

    suspend fun fetch(blockchain: BlockchainDto, id: String, original: T?): T?
}
