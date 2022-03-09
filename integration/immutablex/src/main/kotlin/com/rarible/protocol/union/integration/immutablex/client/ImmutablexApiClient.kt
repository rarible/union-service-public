package com.rarible.protocol.union.integration.immutablex.client

import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexAsset
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexAssetsPage
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexMintsPage
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient

class ImmutablexApiClient(
    private val webClient: WebClient,
    private val apiUrl: String
) {

    suspend fun getAsset(itemId: String): ImmutablexAsset {
        val (collection, tokenId) = itemId.split(":")
        return webClient.get().uri("$apiUrl/assets/${collection}/${tokenId}?include_fees=true")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(ImmutablexAsset::class.java)
            .awaitSingle().body!!
    }

    suspend fun getAllAssets(continuation: String?, size: Int, lastUpdatedTo: Long?, lastUpdatedFrom: Long?): ImmutablexAssetsPage {
        val query = StringBuilder("?order_by=updated_at&direction=desc&include_fees=true")
        query.append("&page_size=$size")
        if (lastUpdatedFrom != null) {
            query.append("&updated_min_timestamp=$lastUpdatedFrom")
        }
        if (lastUpdatedTo != null) {
            query.append("&updated_max_timestamp=$lastUpdatedTo")
        }
        if (!continuation.isNullOrEmpty()) {
            query.append("&cursor=$continuation")
        }
        return webClient.get().uri("$apiUrl/assets$query").accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(ImmutablexAssetsPage::class.java)
            .awaitSingle().body!!

    }

    suspend fun getAssetsByCollection(
        collection: String,
        owner: String?,
        continuation: String?,
        size: Int,
    ): ImmutablexAssetsPage {
        val query = StringBuilder("?order_by=updated_at&direction=desc&include_fees=true")
        query.append("&collection=$collection")
        query.append("&page_size=$size")
        if (!owner.isNullOrEmpty()) {
            query.append("&user=$owner")
        }
        if (!continuation.isNullOrEmpty()) {
            query.append("&cursor=$continuation")
        }
        return webClient.get().uri("$apiUrl/assets$query").accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(ImmutablexAssetsPage::class.java)
            .awaitSingle().body!!
    }

    suspend fun getAssetsByOwner(owner: String, continuation: String?, size: Int): ImmutablexAssetsPage {
        val query = StringBuilder("?order_by=updated_at&direction=desc&include_fees=true")
        query.append("&user=$owner")
        query.append("&page_size=$size")
        if (!continuation.isNullOrEmpty()) {
            query.append("&cursor=$continuation")
        }
        return webClient.get().uri("$apiUrl/assets$query").accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(ImmutablexAssetsPage::class.java)
            .awaitSingle().body!!
    }

    suspend fun getAssetsByIds(itemIds: List<String>): List<ImmutablexAsset> {
        val list = itemIds.map {
            coroutineScope {
                async(Dispatchers.IO) {  getAsset(it)  }
            }
        }
        return list.awaitAll()
    }

    suspend fun getAssetsByCreator(creator: String, continuation: String?, size: Int): ImmutablexAssetsPage {
        val query = StringBuilder("?page_size=$size&user=$creator")
        if (!continuation.isNullOrEmpty()) {
            query.append("&cursor=$continuation")
        }

        val mints = webClient.get().uri("$apiUrl/mints$query").accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(ImmutablexMintsPage::class.java)
            .awaitSingle().body!!

        return ImmutablexAssetsPage(
            cursor = mints.cursor,
            remaining = mints.remaining,
            result = getAssetsByIds(
                mints.result.map { it.token.data.tokenId!! }
            )
        )
    }

    suspend fun getOrderById(id: Long): ImmutablexOrder {
        return webClient.get().uri("$apiUrl/orders/$id?include_fees=true")
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(ImmutablexOrder::class.java)
            .awaitSingle().body!!
    }
}
