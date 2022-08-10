package com.rarible.protocol.union.integration.immutablex.client

import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.parser.IdParser
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant

class ImmutablexActivityClient(
    webClient: WebClient,
) : AbstractImmutablexClient(
    webClient
) {

    companion object {

        val MINTS_ARRAY_TYPE = object : ParameterizedTypeReference<List<ImmutablexMint>>() {}
    }

    // TODO IMMUTABLEX move out to configuration
    private val creatorsRequestChunkSize = 16
    private val byIdsChunkSize = 16

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

    // TODO IMMUTABLEX - transfers can contain burns, there is no way to filter them from regular transfers
    suspend fun getTransfers(
        pageSize: Int,
        continuation: String? = null,
        token: String? = null,
        tokenId: String? = null,
        from: Instant? = null,
        to: Instant? = null,
        user: String? = null,
        sort: ActivitySortDto?
    ) = getActivities<ImmutablexTransfersPage>(
        pageSize,
        continuation,
        token,
        tokenId,
        from,
        to,
        user,
        sort,
        ActivityType.TRANSFER
    ) ?: ImmutablexTransfersPage.EMPTY

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
    ) = getActivities<ImmutablexTradesPage>(
        pageSize,
        continuation,
        token,
        tokenId,
        from,
        to,
        user,
        sort,
        ActivityType.TRADE
    ) ?: ImmutablexTradesPage.EMPTY

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
    ) = webClient.get()
        .uri {
            val builder = ImmutablexActivityQueryBuilder.getQueryBuilder(type, it)
            val safeSort = sort ?: ActivitySortDto.LATEST_FIRST

            builder.token(token)
            builder.tokenId(tokenId)
            builder.user(user)
            builder.pageSize(pageSize)
            // Sorting included
            builder.continuation(from, to, safeSort, continuation)
            builder.build()
        }
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .toEntity(T::class.java).awaitSingle().body

    private suspend inline fun <reified T> getActivityEvents(
        pageSize: Int,
        transactionId: String,
        type: ActivityType
    ) = webClient.get()
        .uri {
            val builder = ImmutablexActivityQueryBuilder.getQueryBuilder(type, it)

            builder.pageSize(pageSize)
            // Sorting included
            builder.continuation(transactionId)
            builder.build()
        }
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .toEntity(T::class.java).awaitSingle().body

    suspend fun getItemCreator(itemId: String): String? {
        val (token, tokenId) = IdParser.split(itemId, 2)
        return getMints(pageSize = 1, token = token, tokenId = tokenId).result.firstOrNull()?.user
    }

    suspend fun getItemCreators(assetIds: Collection<String>): Map<String, String> {
        return getChunked(creatorsRequestChunkSize, assetIds) { itemId ->
            getItemCreator(itemId)?.let { Pair(itemId, it) }
        }.associateBy({ it.first }, { it.second })
    }
}