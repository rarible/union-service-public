package com.rarible.protocol.union.integration.immutablex.client

import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexDeposit
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexMint
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexOrder
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexPage
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexTrade
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexTransfer
import com.rarible.protocol.union.integration.immutablex.dto.ImmutablexWithdrawal
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.toEntity
import org.springframework.web.util.UriBuilder

class EventsApi(
    private val webClient: WebClient
) {

    suspend fun mints(cursor: String? = null): ImmutablexPage<ImmutablexMint> {
        return webClient.get().uri { builder ->
            builder.path("/mints")
            builder.defaultQueryParams()
            if (cursor != null) {
                builder.queryParam("cursor", cursor)
            }
            builder.build()
        }.retrieve().toEntity<ImmutablexPage<ImmutablexMint>>().awaitSingle().body!!
    }

    suspend fun transfers(cursor: String? = null): ImmutablexPage<ImmutablexTransfer> {
        return webClient.get().uri { builder ->
            builder.path("/transfers")
            builder.defaultQueryParams()
            builder.queryParam("token_type", "ERC721")
            if (cursor != null) {
                builder.queryParam("cursor", cursor)
            }
            builder.build()
        }.retrieve().toEntity<ImmutablexPage<ImmutablexTransfer>>().awaitSingle().body!!
    }

    suspend fun trades(cursor: String? = null): ImmutablexPage<ImmutablexTrade> {
        return webClient.get().uri { builder ->
            builder.path("/trades")
            builder.defaultQueryParams()
            builder.queryParam("party_b_token_type", "ERC721")
            if (cursor != null) {
                builder.queryParam("cursor", cursor)
            }
            builder.build()
        }.retrieve().toEntity<ImmutablexPage<ImmutablexTrade>>().awaitSingle().body!!
    }

    suspend fun deposits(cursor: String? = null): ImmutablexPage<ImmutablexDeposit> {
        return webClient.get().uri { builder ->
            builder.path("/deposits")
            builder.defaultQueryParams()
            builder.queryParam("token_type", "ERC721")
            if (cursor != null) {
                builder.queryParam("cursor", cursor)
            }
            builder.build()
        }.retrieve().toEntity<ImmutablexPage<ImmutablexDeposit>>().awaitSingle().body!!

    }

    suspend fun withdrawals(cursor: String? = null): ImmutablexPage<ImmutablexWithdrawal> {
        return webClient.get().uri { builder ->
            builder.path("/withdrawals")
            builder.defaultQueryParams()
            builder.queryParam("token_type", "ERC721")
            if (cursor != null) {
                builder.queryParam("cursor", cursor)
            }
            builder.build()
        }.retrieve().toEntity<ImmutablexPage<ImmutablexWithdrawal>>().awaitSingle().body!!

    }

    suspend fun orders(cursor: String? = null): ImmutablexPage<ImmutablexOrder> {
        return webClient.get().uri { builder ->
            builder.path("/orders")
            builder.defaultQueryParams()
            builder.queryParam("sell_token_type", "ERC721")
            builder.queryParam("order_by", "created_at")
            builder.queryParam("direction", "DESC")
            if (cursor != null) {
                builder.queryParam("cursor", cursor)
            }
            builder.build()
        }.retrieve().toEntity<ImmutablexPage<ImmutablexOrder>>().awaitSingle().body!!
    }

    private fun UriBuilder.defaultQueryParams() {
        this.queryParam("page_size", PAGE_SIZE)
        this.queryParam("order_by", ORDER_BY)
        this.queryParam("direction", ORDER_DIRECTION)
    }

    companion object {
        private const val PAGE_SIZE = 5000
        private const val ORDER_BY = "transaction_id"
        private const val ORDER_DIRECTION = "asc"
    }
}


