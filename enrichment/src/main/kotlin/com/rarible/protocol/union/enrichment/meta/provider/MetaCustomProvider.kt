package com.rarible.protocol.union.enrichment.meta.provider

import com.rarible.protocol.union.dto.BlockchainDto

interface MetaCustomProvider<T> {

    suspend fun fetch(blockchain: BlockchainDto, id: String): Result<T>

    data class Result<T>(
        val supported: Boolean,
        val data: T? = null
    )
}
