package com.rarible.protocol.union.integration.immutablex.client

import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexAsset
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexAssetsPage
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.time.Instant

class ImmutablexAssetClient(
    webClient: WebClient,
) : AbstractImmutablexClient(
    webClient
) {

    // TODO IMMUTABLEX move out to configuration
    private val assetsRequestChunkSize = 16

    suspend fun getAsset(itemId: String): ImmutablexAsset {
        val (collection, tokenId) = IdParser.split(itemId, 2)
        return webClient.get().uri("/assets/${collection}/${tokenId}?include_fees=true")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(ImmutablexAsset::class.java)
            .awaitSingle().body!!
    }

    suspend fun getAssetsByIds(itemIds: List<String>): List<ImmutablexAsset> {
        return getChunked(assetsRequestChunkSize, itemIds) {
            try {
                getAsset(it)
            } catch (e: WebClientResponseException.NotFound) {
                null
            }
        }
    }

    suspend fun getAllAssets(
        continuation: String?,
        size: Int,
        lastUpdatedTo: Long?,
        lastUpdatedFrom: Long?,
    ) = getAssets {
        it.pageSize(size)
        it.continuation(
            lastUpdatedFrom?.let { ms -> Instant.ofEpochMilli(ms) },
            lastUpdatedTo?.let { ms -> Instant.ofEpochMilli(ms) },
            continuation
        )
    }

    suspend fun getAssetsByCollection(
        collection: String,
        owner: String?,
        continuation: String?,
        size: Int,
    ) = getAssets {
        it.owner(owner)
        it.collection(collection)
        it.pageSize(size)
        it.continuation(null, null, continuation)
    }

    suspend fun getAssetsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ) = getAssets {
        it.owner(owner)
        it.pageSize(size)
        it.continuation(null, null, continuation)
    }

    private suspend fun getAssets(build: (builder: ImmutablexAssetQueryBuilder) -> Unit): ImmutablexAssetsPage {
        return webClient.get().uri {
            val builder = ImmutablexAssetQueryBuilder(it)
            build(builder)
            builder.build()
        }.accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(ImmutablexAssetsPage::class.java)
            .awaitSingle().body!!
    }

}