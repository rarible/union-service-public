package com.rarible.protocol.union.enrichment.meta.provider

import com.rarible.protocol.union.dto.BlockchainDto

/**
 * Custom provider of metadata. Can be used for cases where meta resolution can't be
 * executed by default, "global" providers. Such providers should be implemented for
 * specific items/collections. In case when custom provider support meta resolution for
 * provided entity, default providers WON'T be triggered.
 */
interface MetaCustomProvider<T> {

    suspend fun fetch(blockchain: BlockchainDto, id: String): Result<T>

    data class Result<T>(
        // "true" - custom provider supports received entity, "global" provider won't be called
        val supported: Boolean,
        val data: T? = null
    )
}
