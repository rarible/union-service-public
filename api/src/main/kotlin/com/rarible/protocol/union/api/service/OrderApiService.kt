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
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.ext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class OrderApiService(
    private val router: BlockchainRouter<OrderService>
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    suspend fun getByIds(ids: List<OrderIdDto>): List<OrderDto> {
        logger.info("Getting orders by IDs: [{}]", ids.map { "${it.blockchain}:${it.value}" })
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
        val currencyAssetTypes = router.getService(blockchain)
            .getSellCurrencies(contract, tokenId)
            .map { it.ext.currencyAddress() }

        if (currencyAssetTypes.isEmpty()) {
            return Slice.empty()
        }

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

    suspend fun getOrdersAll(
        blockchains: List<BlockchainDto>?,
        continuation: String?,
        size: Int,
        sort: OrderSortDto?,
        status: List<OrderStatusDto>?
    ): Slice<OrderDto> {
        val evaluatedBlockchains = router.getEnabledBlockchains(blockchains).map(BlockchainDto::name)
        val slices = getOrdersByBlockchains(continuation, evaluatedBlockchains) { blockchain, continuation ->
            val blockDto = BlockchainDto.valueOf(blockchain)
            router.getService(blockDto).getOrdersAll(continuation, size, sort, status)
        }
        return ArgPaging(OrderContinuation.ByLastUpdatedAndId, slices).getSlice(size)
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
        val currencyContracts = router.getService(blockchain)
            .getBidCurrencies(contract, tokenId)
            .map { it.ext.currencyAddress() }

        if (currencyContracts.isEmpty()) {
            return Slice.empty()
        }

        val currencySlices = getMultiCurrencyOrdersForItem(
            continuation, currencyContracts
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
        currencyAssetTypes: List<String>,
        clientCall: suspend (currency: String, continuation: String?) -> Slice<OrderDto>
    ): List<ArgSlice<OrderDto>> {

        val currentContinuation = CombinedContinuation.parse(continuation)

        return coroutineScope {
            currencyAssetTypes.map { currency ->
                async {
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

    private suspend fun getOrdersByBlockchains(
        continuation: String?,
        blockchains: Collection<String>,
        clientCall: suspend (blockchain: String, continuation: String?) -> Slice<OrderDto>
    ): List<ArgSlice<OrderDto>> {
        val currentContinuation = CombinedContinuation.parse(continuation)
        return coroutineScope {
            blockchains.map { blockchain ->
                async {
                    val blockchainContinuation = currentContinuation.continuations[blockchain]
                    // For completed blockchain we do not request orders
                    if (blockchainContinuation == ArgSlice.COMPLETED) {
                        ArgSlice(blockchain, blockchainContinuation, Slice(null, emptyList()))
                    } else {
                        ArgSlice(blockchain, blockchainContinuation, clientCall(blockchain, blockchainContinuation))
                    }
                }
            }
        }.awaitAll()
    }

}
