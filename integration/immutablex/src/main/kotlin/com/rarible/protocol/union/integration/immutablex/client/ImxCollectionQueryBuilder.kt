package com.rarible.protocol.union.integration.immutablex.client

import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import org.springframework.web.util.UriBuilder

class ImxCollectionQueryBuilder(
    builder: UriBuilder
) : AbstractImxQueryBuilder(
    builder, PATH
) {

    companion object {

        const val PATH = "/collections"

        fun getByIdPath(collection: String): String {
            return "$PATH/$collection"
        }

        fun getMetaSchemaPath(collection: String): String {
            return "$PATH/$collection/metadata-schema"
        }
    }

    fun continuationById(continuation: String?) {
        // Hack for IMX cursor - they use base64 of JSON of the last entity at the page,
        // but we can pass here only one field to make cursor acceptable - address
        val cursor = continuation
            ?.let { ImxCursor.encode("""{"address":"$continuation"}""") }

        builder.queryParamNotNull("cursor", cursor)
        orderBy("address", "asc")
    }

    fun continuationByUpdatedAt(continuation: String?, sortAsc: Boolean) {
        val cursor = continuation
            ?.let { DateIdContinuation.parse(continuation) }
            ?.let { parsed ->
                // Since IMX using microseconds in their cursors while we're using ms,
                // there is no way to avoid duplication on page break except increasing/decreasing date from our TS
                val fixedDate = when (sortAsc) {
                    true -> parsed.date.plusMillis(1)
                    false -> parsed.date.minusMillis(1)
                }
                ImxCursor.encode("""{"address":"${parsed.id}","updated_at":"$fixedDate"}""")
            }

        val direction = when (sortAsc) {
            true -> "asc"
            false -> "desc"
        }

        cursor(cursor)
        orderBy("updated_at", direction)
    }
}
