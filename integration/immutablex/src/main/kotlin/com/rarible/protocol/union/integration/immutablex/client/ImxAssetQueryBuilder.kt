package com.rarible.protocol.union.integration.immutablex.client

import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.dto.parser.IdParser
import org.apache.commons.codec.binary.Base64
import org.springframework.web.util.UriBuilder
import java.time.Instant

class ImxAssetQueryBuilder(
    builder: UriBuilder
) : AbstractImxQueryBuilder(
    builder, PATH
) {

    companion object {

        const val PATH = "/assets"

        fun getByIdPath(itemId: String): String {
            val (collection, tokenId) = IdParser.split(itemId, 2)
            return "$PATH/${collection}/${tokenId}?include_fees=true"
        }
    }

    init {
        builder.queryParam("include_fees", true)
    }

    fun collection(collection: String?) {
        builder.queryParamNotNull("collection", collection)
    }

    fun owner(owner: String?) {
        builder.queryParamNotNull("user", owner)
    }

    fun fromDate(from: Instant?) {
        builder.queryParamNotNull("updated_min_timestamp", from)
    }

    fun toDate(to: Instant?) {
        builder.queryParamNotNull("updated_max_timestamp", to)
    }

    fun continuation(continuation: String?, sortAsc: Boolean = true) {
        val cursor = continuation
            ?.let { DateIdContinuation.parse(continuation) }
            ?.let { parsed ->

                val date = parsed.date
                val (token, tokenId) = IdParser.split(parsed.id, 2)

                // Since IMX using microseconds in their cursors while we're using ms,
                // there is no way to avoid duplication on page break except increasing/decreasing date from our TS
                val fixedDate = when (sortAsc) {
                    true -> date.plusMillis(1)
                    false -> date.minusMillis(1)
                }
                Base64.encodeBase64String(
                    """{"contract_address":"$token","client_token_id":"$tokenId","updated_at":"$fixedDate"}"""
                        .toByteArray()
                ).trimEnd('=')
            }

        val direction = when (sortAsc) {
            true -> "asc"
            false -> "desc"
        }

        cursor(cursor)
        orderBy("updated_at", direction)
    }
}