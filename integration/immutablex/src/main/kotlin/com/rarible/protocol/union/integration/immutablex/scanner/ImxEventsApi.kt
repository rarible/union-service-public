package com.rarible.protocol.union.integration.immutablex.scanner

import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexActivityClient
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexMint
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrder
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrderClient
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTrade
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTransfer
import java.time.Instant

class ImxEventsApi(
    private val activityClient: ImmutablexActivityClient,
    private val orderClient: ImmutablexOrderClient
) {

    suspend fun mints(transactionId: String): List<ImmutablexMint> {
        return activityClient.getMintEvents(
            pageSize = PAGE_SIZE,
            transactionId = transactionId
        )
    }

    suspend fun lastMint(): ImmutablexMint {
        return activityClient.getLastMint()
    }

    suspend fun transfers(transactionId: String): List<ImmutablexTransfer> {
        return activityClient.getTransferEvents(
            pageSize = PAGE_SIZE,
            transactionId = transactionId
        )
    }

    suspend fun lastTransfer(): ImmutablexTransfer {
        return activityClient.getLastTransfer()
    }

    suspend fun trades(transactionId: String): List<ImmutablexTrade> {
        return activityClient.getTradeEvents(
            pageSize = PAGE_SIZE,
            transactionId = transactionId
        )
    }

    suspend fun lastTrade(): ImmutablexTrade {
        return activityClient.getLastTrade()
    }

    /*
    suspend fun deposits(cursor: String? = null): ImmutablexPage<ImmutablexDeposit> {
        return webClient.get().uri {
            it.path("/deposits")
                .defaultQueryParams()
                .queryParam("token_type", "ERC721")
                .queryParamNotNull("cursor", cursor)
                .build()
        }.retrieve().toEntity<ImmutablexPage<ImmutablexDeposit>>().awaitSingle().body!!
    }


    suspend fun withdrawals(date: Instant, id: String): ImmutablexPage<ImmutablexWithdrawal> {
        return webClient.get().uri {
            it.path("/withdrawals")
                .defaultQueryParams()
                .queryParam("token_type", "ERC721")
                .queryParamNotNull("cursor", cursor)
                .build()
        }.retrieve().toEntity<ImmutablexPage<ImmutablexWithdrawal>>().awaitSingle().body!!
    }
    */

    suspend fun orders(date: Instant, id: String): List<ImmutablexOrder> {
        return orderClient.getAllOrders(
            continuation = "${date.toEpochMilli()}_${id}",
            size = PAGE_SIZE,
            sort = OrderSortDto.LAST_UPDATE_ASC,
            statuses = null
        )
    }


    companion object {

        private const val PAGE_SIZE = 200 // As per IMX support, that's maximum page size
    }
}


