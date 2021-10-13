package com.rarible.protocol.union.api.service

import com.rarible.protocol.union.core.continuation.CombinedContinuation
import com.rarible.protocol.union.core.continuation.OrderContinuation
import com.rarible.protocol.union.core.continuation.page.ArgPaging
import com.rarible.protocol.union.core.continuation.page.ArgSlice
import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.service.OrderService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderIdDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.enrichment.model.ShortItemId
import com.rarible.protocol.union.enrichment.service.EnrichmentItemService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Component
import java.math.BigInteger

@Component
class OrderApiService(
    private val router: BlockchainRouter<OrderService>,
    private val enrichmentItemService: EnrichmentItemService
) {

    suspend fun getByIds(ids: List<OrderIdDto>): List<OrderDto> {
        val groupedIds = ids.groupBy({ it.blockchain }, { it.value })

        return groupedIds.flatMap {
            router.getService(it.key).getOrdersByIds(it.value)
        }
    }

    suspend fun getSellOrdersByItem(
        blockchain: BlockchainDto,
        platform: PlatformDto?,
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        status: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val shortItemId = ShortItemId(blockchain, contract, BigInteger(tokenId))
        val shortItem = enrichmentItemService.get(shortItemId) ?: return Slice(null, emptyList())
        val currencyAssetTypes = shortItem.bestSellOrders.keys.map { UnionAddress(shortItem.blockchain, it) }

        val currencySlices = getMultiCurrencyOrdersForItem(
            continuation, currencyAssetTypes
        ) { currency, currencyContinuation ->
            router.getService(blockchain).getSellOrdersByItem(
                platform, contract, tokenId, maker, origin, status, currency, currencyContinuation, size
            )
        }

        // Here we're sorting orders using price evaluated in USD (DESC)
        return ArgPaging(
            OrderContinuation.BySellPriceUsdAndIdAsc,
            currencySlices
        ).getSlice(size)
    }

    suspend fun getOrderBidsByItem(
        blockchain: BlockchainDto,
        platform: PlatformDto?,
        contract: String,
        tokenId: String,
        maker: String?,
        origin: String?,
        status: List<OrderStatusDto>?,
        start: Long?,
        end: Long?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val shortItemId = ShortItemId(blockchain, contract, BigInteger(tokenId))
        val shortItem = enrichmentItemService.get(shortItemId) ?: return Slice(null, emptyList())
        val currencyAssetTypes = shortItem.bestBidOrders.keys.map { UnionAddress(shortItem.blockchain, it) }

        val currencySlices = getMultiCurrencyOrdersForItem(
            continuation, currencyAssetTypes
        ) { currency, currencyContinuation ->
            router.getService(blockchain).getOrderBidsByItem(
                platform, contract, tokenId, maker, origin, status, start, end, currency, currencyContinuation, size
            )
        }

        // Here we're sorting orders using price evaluated in USD (DESC)
        return ArgPaging(
            OrderContinuation.ByBidPriceUsdAndIdDesc,
            currencySlices
        ).getSlice(size)
    }

    private suspend fun getMultiCurrencyOrdersForItem(
        continuation: String?,
        currencyAssetTypes: List<UnionAddress>,
        clientCall: suspend (currency: String, continuation: String?) -> Slice<OrderDto>
    ): List<ArgSlice<OrderDto>> {

        val currentContinuation = CombinedContinuation.parse(continuation)

        return coroutineScope {
            currencyAssetTypes.map {
                async {
                    val currency = it.value
                    val currencyContinuation = currentContinuation.continuations[currency]
                    // For completed currencies we do not request orders
                    if (currencyContinuation == ArgSlice.COMPLETED) {
                        ArgSlice(currency, currencyContinuation, Slice(null, emptyList()))
                    } else {
                        ArgSlice(currency, currencyContinuation, clientCall(currency, currencyContinuation))
                    }
                }
            }
        }.awaitAll()
    }

}