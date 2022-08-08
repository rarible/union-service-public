package com.rarible.protocol.union.integration.immutablex.client

import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import org.springframework.web.util.UriBuilder
import java.time.temporal.ChronoUnit

class ImmutablexOrderQueryBuilder(
    builder: UriBuilder
) : AbstractImmutablexQueryBuilder(
    builder, PATH
) {

    companion object {

        private const val PATH = "/orders"

        fun getByIdPath(orderId: String): String {
            return "$PATH/$orderId?include_fees=true"
        }
    }

    init {
        builder.queryParam("include_fees", true)
    }

    fun sellTokenType(tokenType: String) {
        builder.queryParam("sell_token_type", tokenType)
    }

    fun buyTokenType(tokenType: String) {
        builder.queryParam("buy_token_type", tokenType)
    }

    fun sellToken(collection: String?) {
        builder.queryParamNotNull("sell_token_address", collection)
    }

    fun sellTokenId(tokenId: String?) {
        builder.queryParamNotNull("sell_token_id", tokenId)
    }

    fun buyToken(collection: String?) {
        builder.queryParamNotNull("buy_token_address", collection)
    }

    fun buyTokenId(tokenId: String?) {
        builder.queryParamNotNull("buy_token_id", tokenId)
    }

    fun maker(maker: String?) {
        builder.queryParamNotNull("user", maker)
    }

    fun status(status: OrderStatusDto?) {
        val immStatus = when (status) {
            null -> null
            OrderStatusDto.HISTORICAL -> null // Not supported by Immutablex
            // TODO we need to consider Expired status here somehow
            else -> status.name.lowercase()
        }
        builder.queryParamNotNull("status", immStatus)
    }

    // TODO ideally we need to hack the cursor here (like it done for collections)
    fun continuation(sort: OrderSortDto, continuation: String?) {
        val continuationDate = DateIdContinuation.parse(continuation)?.date

        // TODO ugly hack until we haven't cursor
        // Have no idea what to do with TS... IMX doesn't exclude date from continuation,
        // and we're getting duplicated items on the page break
        val queryFrom = when (sort) {
            OrderSortDto.LAST_UPDATE_ASC -> continuationDate?.plus(1, ChronoUnit.MILLIS)
            OrderSortDto.LAST_UPDATE_DESC -> null
        }
        val queryTo = when (sort) {
            OrderSortDto.LAST_UPDATE_ASC -> null
            OrderSortDto.LAST_UPDATE_DESC -> continuationDate?.minus(1, ChronoUnit.MILLIS)
        }
        val direction = when (sort) {
            OrderSortDto.LAST_UPDATE_ASC -> "asc"
            OrderSortDto.LAST_UPDATE_DESC -> "desc"
        }

        builder.queryParamNotNull("updated_min_timestamp", queryFrom)
        builder.queryParamNotNull("updated_max_timestamp", queryTo)

        orderBy("updated_at", direction)
    }

    fun sellPriceContinuation(currencyId: String, continuation: String?) {
        val price = continuation?.substringBefore("_")

        when (isEth(currencyId)) {
            true -> buyTokenType("ETH")
            false -> buyToken(currencyId)
        }

        builder.queryParamNotNull("buy_min_quantity", price)

        orderBy("buy_quantity", "asc")
    }

    fun buyPriceContinuation(currencyId: String, continuation: String?) {
        val price = continuation?.substringBefore("_")

        when (isEth(currencyId)) {
            true -> sellTokenType("ETH")
            false -> sellToken(currencyId)
        }
        builder.queryParamNotNull("sell_max_quantity", price)
        orderBy("sell_quantity", "desc")
    }

    private fun isEth(currencyId: String): Boolean {
        return (currencyId == "0x0000000000000000000000000000000000000000")
    }

}