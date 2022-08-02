package com.rarible.protocol.union.integration.immutablex.client

import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import org.springframework.web.util.UriBuilder
import java.time.Instant

class ImmutablexAssetQueryBuilder(
    builder: UriBuilder
) : AbstractImmutablexQueryBuilder(
    builder
) {

    init {
        builder.path("/assets")
            .queryParam("include_fees", true)
    }

    fun collection(collection: String?) {
        builder.queryParamNotNull("collection", collection)
    }

    fun owner(owner: String?) {
        builder.queryParamNotNull("user", owner)
    }

    fun continuation(from: Instant?, to: Instant?, continuation: String?) {
        val continuationDate = DateIdContinuation.parse(continuation)?.date

        val queryTo = listOfNotNull(to, continuationDate).minOrNull()

        builder.queryParamNotNull("updated_min_timestamp", from)
        builder.queryParamNotNull("updated_max_timestamp", queryTo)

        orderBy("updated_at", "desc")
    }

}