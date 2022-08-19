package com.rarible.protocol.union.integration.immutablex.client

import com.rarible.protocol.union.dto.ActivitySortDto
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriBuilder
import java.time.Instant

class ImxActivityClient(
    webClient: WebClient,
    private val byIdsChunkSize: Int
) : AbstractImxClient(
    webClient
) {

    companion object {

        val MINTS_ARRAY_TYPE = object : ParameterizedTypeReference<List<ImmutablexMint>>() {}
    }

    suspend fun getMints(ids: List<String>): List<ImmutablexMint> {
        return getChunked(byIdsChunkSize, ids) {
            ignore404 {
                // For some reason IMX returns array with one element instead of single object
                getByUri(MintQueryBuilder.getByIdPath(it), MINTS_ARRAY_TYPE).firstOrNull()
            }
        }
    }

    suspend fun getLastMint(): ImmutablexMint {
        val result = getActivities<ImmutablexMintsPage>(
            pageSize = 1,
            sort = ActivitySortDto.LATEST_FIRST,
            type = ActivityType.MINT
        )
        return result!!.result.first()
    }

    suspend fun getMintEvents(pageSize: Int, transactionId: String): List<ImmutablexMint> {
        return getActivityEvents<ImmutablexMintsPage>(
            pageSize,
            transactionId,
            ActivityType.MINT
        )?.result ?: emptyList()
    }

    suspend fun getMints(
        pageSize: Int,
        continuation: String? = null,
        token: String? = null,
        tokenId: String? = null,
        from: Instant? = null,
        to: Instant? = null,
        user: String? = null,
        sort: ActivitySortDto? = null
    ) = getActivities<ImmutablexMintsPage>(
        pageSize,
        continuation,
        token,
        tokenId,
        from,
        to,
        user,
        sort,
        ActivityType.MINT
    ) ?: ImmutablexMintsPage.EMPTY

    suspend fun getTransfers(ids: List<String>): List<ImmutablexTransfer> {
        return getChunked(byIdsChunkSize, ids) {
            ignore404 {
                getByUri<ImmutablexTransfer>(TransferQueryBuilder.getByIdPath(it))
            }
        }
    }

    // Also, may include Burns, but for scanner it is totally fine
    suspend fun getLastTransfer(): ImmutablexTransfer {
        val result = getActivities<ImmutablexTransfersPage>(
            pageSize = 1,
            sort = ActivitySortDto.LATEST_FIRST,
            type = ActivityType.TRANSFER
        )
        return result!!.result.first()
    }

    suspend fun getTransferEvents(pageSize: Int, transactionId: String): List<ImmutablexTransfer> {
        return getActivityEvents<ImmutablexTransfersPage>(
            pageSize,
            transactionId,
            ActivityType.TRANSFER
        )?.result ?: emptyList()
    }

    suspend fun getTransfers(
        pageSize: Int,
        continuation: String? = null,
        token: String? = null,
        tokenId: String? = null,
        from: Instant? = null,
        to: Instant? = null,
        user: String? = null,
        transferFilter: TransferFilter,
        sort: ActivitySortDto?
    ): ImmutablexTransfersPage {
        return when (transferFilter) {
            // Both, Transfers and Burns will be included into response
            TransferFilter.ALL -> {
                getActivities<ImmutablexTransfersPage>(
                    pageSize, continuation, token, tokenId, from, to, user, sort, ActivityType.TRANSFER
                )
            }
            // Only burns will be included into the response
            // There is no way to get transfers only without burns
            TransferFilter.BURNS -> {
                getActivities<ImmutablexTransfersPage> {
                    val builder = TransferQueryBuilder(it)
                    val safeSort = sort ?: ActivitySortDto.LATEST_FIRST

                    builder.token(token)
                    builder.tokenId(tokenId)
                    builder.user(user)
                    builder.receiver(ImmutablexTransfer.ZERO_ADDRESS)
                    builder.pageSize(pageSize)
                    builder.continuationByDate(from, to, safeSort, continuation)
                    builder.build()
                    builder.build()
                }
            }
        } ?: ImmutablexTransfersPage.EMPTY
    }

    suspend fun getTrades(ids: List<String>): List<ImmutablexTrade> {
        return getChunked(byIdsChunkSize, ids) {
            ignore404 {
                getByUri<ImmutablexTrade>(TradeQueryBuilder.getByIdPath(it))
            }
        }
    }

    suspend fun getLastTrade(): ImmutablexTrade {
        val result = getActivities<ImmutablexTradesPage>(
            pageSize = 1,
            sort = ActivitySortDto.LATEST_FIRST,
            type = ActivityType.TRADE
        )
        return result!!.result.first()
    }

    suspend fun getTradeEvents(pageSize: Int, transactionId: String): List<ImmutablexTrade> {
        return getActivityEvents<ImmutablexTradesPage>(
            pageSize,
            transactionId,
            ActivityType.TRADE
        )?.result ?: emptyList()
    }

    suspend fun getTrades(
        pageSize: Int,
        continuation: String? = null,
        token: String? = null,
        tokenId: String? = null,
        from: Instant? = null,
        to: Instant? = null,
        user: String? = null,
        sort: ActivitySortDto?
    ): ImmutablexTradesPage {
        // IMX has problems with trades requests where page_size=1 and filters by itemId,
        // with page_size=200 it works much faster, should be fixed by IMX one day
        val hackedPageSize = if (
            pageSize == 1
            && token != null
            && tokenId != null
            && sort == ActivitySortDto.LATEST_FIRST) {
            200
        } else {
            pageSize
        }
        val page = getActivities<ImmutablexTradesPage>(
            hackedPageSize,
            continuation,
            token,
            tokenId,
            from,
            to,
            user,
            sort,
            ActivityType.TRADE
        ) ?: ImmutablexTradesPage.EMPTY

        return if (hackedPageSize == pageSize) {
            page
        } else {
            val result = page.result.firstOrNull()
            ImmutablexTradesPage("", false, listOfNotNull(result))
        }
    }

    private suspend inline fun <reified T> getActivities(
        pageSize: Int,
        continuation: String? = null,
        token: String? = null,
        tokenId: String? = null,
        from: Instant? = null,
        to: Instant? = null,
        user: String? = null,
        sort: ActivitySortDto? = null,
        type: ActivityType
    ) = getActivities<T> {

        val builder = ImxActivityQueryBuilder.getQueryBuilder(type, it)
        val safeSort = sort ?: ActivitySortDto.LATEST_FIRST

        builder.token(token)
        builder.tokenId(tokenId)
        builder.user(user)
        builder.pageSize(pageSize)
        // Sorting included
        builder.continuationByDate(from, to, safeSort, continuation)
        builder.build()
    }

    private suspend inline fun <reified T> getActivityEvents(
        pageSize: Int,
        transactionId: String,
        type: ActivityType
    ) = getActivities<T> {

        val builder = ImxActivityQueryBuilder.getQueryBuilder(type, it)

        builder.pageSize(pageSize)
        // Sorting included
        builder.continuationById(transactionId)
        builder.build()
    }

    private suspend inline fun <reified T> getActivities(crossinline build: (builder: UriBuilder) -> Unit): T? {
        return webClient.get().uri {
            build(it)
            it.build()
        }.accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .toEntity(T::class.java)
            .awaitSingle().body!!
    }
}

enum class TransferFilter {
    BURNS,
    ALL
}