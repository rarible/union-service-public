package com.rarible.protocol.union.integration.immutablex.client

import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import org.springframework.web.util.UriBuilder

class ImxOrderQueryBuilder(
    builder: UriBuilder
) : AbstractImxQueryBuilder(
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

    fun continuationByUpdatedAt(sort: OrderSortDto, continuation: String?) {
        val cursor = continuation
            ?.let { DateIdContinuation.parse(continuation) }
            ?.let { parsed ->
                // Since IMX using microseconds in their cursors while we're using ms,
                // there is no way to avoid duplication on page break except increasing/decreasing date from our TS
                val fixedDate = when (sort) {
                    OrderSortDto.LAST_UPDATE_ASC -> parsed.date.plusMillis(1)
                    // Nanoseconds already trimmed here, we can keep it as is
                    OrderSortDto.LAST_UPDATE_DESC -> parsed.date
                }
                ImxCursor.encode("""{"order_id":${parsed.id},"updated_at":"$fixedDate"}""")
            }

        builder.queryParamNotNull("cursor", cursor)

        val direction = when (sort) {
            OrderSortDto.LAST_UPDATE_ASC -> "asc"
            OrderSortDto.LAST_UPDATE_DESC -> "desc"
        }

        orderBy("updated_at", direction)
    }

    fun continuationBySellPrice(currencyId: String, continuation: String?) {
        val price = continuation?.substringBefore("_")

        when (isEth(currencyId)) {
            true -> buyTokenType("ETH")
            false -> buyToken(currencyId)
        }

        builder.queryParamNotNull("buy_min_quantity", price)

        orderBy("buy_quantity", "asc")
    }

    fun continuationByBuyPrice(currencyId: String, continuation: String?) {
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
