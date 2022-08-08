package com.rarible.protocol.union.integration.immutablex.client

import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import org.springframework.web.util.UriBuilder
import java.time.Instant

sealed class ImmutablexActivityQueryBuilder(
    builder: UriBuilder,
    path: String
) : AbstractImmutablexQueryBuilder(
    builder,
    path
) {

    abstract val tokenIdField: String
    abstract val tokenField: String

    fun token(token: String?) {
        builder.queryParamNotNull(tokenField, token)
    }

    fun tokenId(tokenId: String?) {
        builder.queryParamNotNull(tokenIdField, tokenId)
    }

    fun user(user: String?) {
        builder.queryParamNotNull("user", user)
    }

    fun continuation(from: Instant?, to: Instant?, sort: ActivitySortDto, continuation: String?) {
        val continuationDate = DateIdContinuation.parse(continuation)?.date

        val queryFrom = when (sort) {
            ActivitySortDto.EARLIEST_FIRST -> listOfNotNull(from, continuationDate).maxOrNull()
            ActivitySortDto.LATEST_FIRST -> from
        }
        val queryTo = when (sort) {
            ActivitySortDto.EARLIEST_FIRST -> to
            ActivitySortDto.LATEST_FIRST -> listOfNotNull(to, continuationDate).minOrNull()
        }
        val direction = when (sort) {
            ActivitySortDto.EARLIEST_FIRST -> "asc"
            ActivitySortDto.LATEST_FIRST -> "desc"
        }

        builder.queryParamNotNull("min_timestamp", queryFrom)
        builder.queryParamNotNull("max_timestamp", queryTo)

        orderBy("created_at", direction)
    }

    companion object {

        fun getQueryBuilder(type: ActivityType, uriBuilder: UriBuilder): ImmutablexActivityQueryBuilder {
            return when (type) {
                ActivityType.MINT -> MintQueryBuilder(uriBuilder)
                ActivityType.TRADE -> TradeQueryBuilder(uriBuilder)
                ActivityType.TRANSFER -> TransferQueryBuilder(uriBuilder)
            }
        }
    }

}

class MintQueryBuilder(
    builder: UriBuilder
) : ImmutablexActivityQueryBuilder(
    builder, PATH
) {

    companion object {

        const val PATH = "/mints"

        fun getByIdPath(id: String): String {
            return "$PATH/${id}"
        }
    }

    override val tokenIdField: String = "token_id"
    override val tokenField: String = "token_address"

}

class TradeQueryBuilder(
    builder: UriBuilder
) : ImmutablexActivityQueryBuilder(
    builder, PATH
) {

    companion object {

        const val PATH = "/trades"

        fun getByIdPath(id: String): String {
            return "$PATH/${id}"
        }
    }

    override val tokenIdField = "party_b_token_id"
    override val tokenField = "party_b_token_address"

}

class TransferQueryBuilder(
    builder: UriBuilder
) : ImmutablexActivityQueryBuilder(
    builder, PATH
) {

    companion object {

        const val PATH = "/transfers"

        fun getByIdPath(id: String): String {
            return "$PATH/${id}"
        }
    }

    override val tokenIdField: String = "token_id"
    override val tokenField: String = "token_address"

}

enum class ActivityType {
    MINT,
    TRANSFER,
    TRADE
}
