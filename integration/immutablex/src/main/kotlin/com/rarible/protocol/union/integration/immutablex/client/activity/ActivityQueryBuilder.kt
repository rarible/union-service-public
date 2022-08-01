package com.rarible.protocol.union.integration.immutablex.client.activity

import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.dto.parser.IdParser
import com.rarible.protocol.union.integration.immutablex.client.AbstractQueryBuilder
import com.rarible.protocol.union.integration.immutablex.client.queryParamNotNull
import org.springframework.web.util.UriBuilder
import java.time.Instant

abstract class ActivityQueryBuilder(builder: UriBuilder) : AbstractQueryBuilder(builder) {

    abstract val tokenIdField: String
    abstract val tokenField: String

    abstract val timestampMaxField: String
    abstract val timestampMinField: String

    fun token(token: String?) {
        builder.queryParamNotNull(tokenField, token)
    }

    fun tokenId(tokenId: String?) {
        builder.queryParamNotNull(tokenIdField, tokenId)
    }

    fun itemId(itemId: String?) {
        itemId ?: return
        val (address, id) = IdParser.split(itemId, 2)
        builder.queryParam(tokenField, address)
        builder.queryParam(tokenIdField, id)
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

        builder.queryParamNotNull(timestampMinField, queryFrom)
        builder.queryParamNotNull(timestampMaxField, queryTo)

        orderBy("updated_at", direction)
    }

}