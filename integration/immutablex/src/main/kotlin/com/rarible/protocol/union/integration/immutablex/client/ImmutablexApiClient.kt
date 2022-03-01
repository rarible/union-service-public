package com.rarible.protocol.union.integration.immutablex.client

import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexAsset
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
}
