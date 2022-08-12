package com.rarible.protocol.union.integration.immutablex.scanner

import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexAsset
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexCollection
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexMint
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexOrder
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTrade
import com.rarible.protocol.union.integration.immutablex.client.ImmutablexTransfer
import com.rarible.protocol.union.integration.immutablex.client.ImxActivityClient
import com.rarible.protocol.union.integration.immutablex.client.ImxAssetClient
import com.rarible.protocol.union.integration.immutablex.client.ImxCollectionClient
import com.rarible.protocol.union.integration.immutablex.client.ImxOrderClient
import java.time.Instant

class ImxEventsApi(
    private val activityClient: ImxActivityClient,
    private val assetClient: ImxAssetClient,
    private val orderClient: ImxOrderClient,
    private val collectionClient: ImxCollectionClient
) {

    suspend fun assets(date: Instant, id: String): List<ImmutablexAsset> {
        return assetClient.getAllAssets(
            continuation = DateIdContinuation(date, id).toString(),
            size = PAGE_SIZE,
            sortAsc = true
        ).result
    }

    suspend fun collections(date: Instant, id: String): List<ImmutablexCollection> {
        return collectionClient.getAllWithUpdateAtSort(
            continuation = DateIdContinuation(date, id).toString(),
            size = PAGE_SIZE,
            sortAsc = true
        ).result
    }

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
            continuation = DateIdContinuation(date, id).toString(),
            size = PAGE_SIZE,
            sort = OrderSortDto.LAST_UPDATE_ASC,
            statuses = null
        )
    }

    companion object {

        private const val PAGE_SIZE = 200 // As per IMX support, that's maximum page size
    }
}


