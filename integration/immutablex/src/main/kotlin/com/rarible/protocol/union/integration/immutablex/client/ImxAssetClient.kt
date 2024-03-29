package com.rarible.protocol.union.integration.immutablex.client

import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant

class ImxAssetClient(
    webClient: WebClient,
    private val byIdsChunkSize: Int
) : AbstractImxClient(
    webClient
) {

    suspend fun getById(itemId: String): ImmutablexAsset {
        val uri = ImxAssetQueryBuilder.getByIdPath(itemId)
        return getByUri(uri)
    }

    suspend fun getByIdOrNull(itemId: String): ImmutablexAsset? {
        val uri = ImxAssetQueryBuilder.getByIdPath(itemId)
        return ignore404 { getByUri(uri) }
    }

    suspend fun getByIds(itemIds: Collection<String>): List<ImmutablexAsset> {
        return getChunked(byIdsChunkSize, itemIds) {
            ignore404 { getById(it) }
        }
    }

    suspend fun getAllAssets(
        continuation: String?,
        size: Int,
        lastUpdatedTo: Long? = null,
        lastUpdatedFrom: Long? = null,
        sortAsc: Boolean,
        showDeleted: Boolean = false
    ) = getAssets {
        if (!showDeleted) {
            it.status("imx")
        }
        it.pageSize(size)
        it.fromDate(lastUpdatedFrom?.let { ms -> Instant.ofEpochMilli(ms) })
        it.toDate(lastUpdatedTo?.let { ms -> Instant.ofEpochMilli(ms) })
        it.continuationByUpdatedAt(continuation, sortAsc)
    }

    suspend fun getAssetsByCollection(
        collection: String,
        owner: String?,
        continuation: String?,
        size: Int,
    ) = getAssets {
        it.owner(owner)
        it.status("imx")
        it.collection(collection)
        it.pageSize(size)
        it.continuationByUpdatedAt(continuation, false)
    }

    suspend fun getAssetsByOwner(
        owner: String,
        continuation: String?,
        size: Int
    ) = getAssets {
        it.owner(owner)
        it.status("imx")
        it.pageSize(size)
        it.continuationByUpdatedAt(continuation, false)
    }

    private suspend fun getAssets(build: (builder: ImxAssetQueryBuilder) -> Unit): ImmutablexAssetsPage {
        return webClient.get().uri {
            val builder = ImxAssetQueryBuilder(it)
            build(builder)
            builder.build()
        }.accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(ImmutablexAssetsPage::class.java)
            .awaitSingle().body!!
    }
}
