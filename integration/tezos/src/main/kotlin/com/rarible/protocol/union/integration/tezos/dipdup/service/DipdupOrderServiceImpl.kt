package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.core.logging.Logger
import com.rarible.dipdup.client.OrderClient
import com.rarible.dipdup.client.core.model.OrderStatus
import com.rarible.dipdup.client.core.model.TezosPlatform
import com.rarible.dipdup.client.exception.DipDupNotFound
import com.rarible.dipdup.client.exception.WrongArgument
import com.rarible.dipdup.client.model.DipDupOrderSort
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.core.exception.UnionValidationException
import com.rarible.protocol.union.core.model.UnionAssetType
import com.rarible.protocol.union.core.model.UnionOrder
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupIntegrationProperties
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupActivityConverter
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupOrderConverter
import java.math.BigInteger

class DipdupOrderServiceImpl(
    private val dipdupOrderClient: OrderClient,
    private val dipDupOrderConverter: DipDupOrderConverter,
    private val marketplaces: DipDupIntegrationProperties.Marketplaces
) : DipdupOrderService {

    private val blockchain = BlockchainDto.TEZOS
    private val enabledPlatforms = marketplaces.getEnabledMarketplaces().toList()

    override suspend fun getOrderById(id: String): UnionOrder {
        logger.info("Fetch dipdup order by id: $id")
        val order = safeApiCall { dipdupOrderClient.getOrderById(id) }
        return dipDupOrderConverter.convert(order, blockchain)
    }

    override suspend fun getOrderByIds(ids: List<String>): List<UnionOrder> {
        logger.info("Fetch dipdup orders by ids: $ids")
        val orders = safeApiCall { dipdupOrderClient.getOrdersByIds(ids) }
        return orders.map { dipDupOrderConverter.convert(it, blockchain) }
    }

    override suspend fun getOrdersAll(
        sort: OrderSortDto?,
        statuses: List<OrderStatusDto>?,
        isBid: Boolean?,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder> {
        logger.info("Fetch dipdup all orders: $sort, $statuses, $continuation, $size")
        val page = safeApiCall {
            dipdupOrderClient.getOrdersAll(
                sort = sort?.let { dipDupOrderConverter.convert(it) },
                statuses = statuses?.let { it.map { status -> dipDupOrderConverter.convert(status) } } ?: emptyList(),
                platforms = enabledPlatforms,
                isBid = isBid,
                size = size,
                continuation = continuation
            )
        }
        return Slice(
            continuation = page.continuation,
            entities = page.orders.map { dipDupOrderConverter.convert(it, blockchain) }
        )
    }

    override suspend fun getOrdersAllSync(continuation: String?, limit: Int, sort: SyncSortDto?): Slice<UnionOrder> {
        val sortTezos = sort?.let { DipDupActivityConverter.convert(it) }
        logger.info("Fetch dipdup all order sync: $continuation, $limit, $sort")
        val page = safeApiCall { dipdupOrderClient.getOrdersSync(limit, continuation, sortTezos) }
        return Slice(
            continuation = page.continuation,
            entities = page.orders.map { dipDupOrderConverter.convert(it, blockchain) }
        )
    }

    override suspend fun getSellOrders(
        origin: String?,
        requested: List<TezosPlatform>,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder> {
        logger.info("Fetch dipdup sell orders: $requested, $origin, $continuation, $size")
        val page = safeApiCall {
            dipdupOrderClient.getOrdersAll(
                sort = DipDupOrderSort.LAST_UPDATE_DESC,
                statuses = listOf(OrderStatus.ACTIVE),
                platforms = platforms(requested),
                isBid = false,
                size = size,
                continuation = continuation
            )
        }
        return Slice(
            continuation = page.continuation,
            entities = page.orders.map { dipDupOrderConverter.convert(it, blockchain) }
        )
    }

    override suspend fun getSellOrdersByCollection(
        contract: String,
        origin: String?,
        platforms: List<TezosPlatform>,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder> {
        logger.info("Fetch dipdup sell orders by collection: $contract, platforms=$platforms, continuation=$continuation, size=$size")
        val page = safeApiCall {
            dipdupOrderClient.getOrdersByCollection(
                contract = contract,
                statuses = listOf(OrderStatus.ACTIVE),
                platforms = platforms(platforms),
                isBid = false,
                size = size,
                continuation = continuation
            )
        }
        return Slice(
            continuation = page.continuation,
            entities = page.orders.map { dipDupOrderConverter.convert(it, blockchain) }
        )
    }

    override suspend fun getSellOrdersByItem(
        contract: String,
        tokenId: BigInteger,
        maker: String?,
        platforms: List<TezosPlatform>,
        currencyId: String,
        statuses: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder> {
        logger.info("Fetch dipdup sell orders by item: $contract:$tokenId, maker=$maker, platforms=$platforms, currency=$currencyId, status=$statuses, continuation=$continuation, size=$size")
        val page = safeApiCall {
            dipdupOrderClient.getOrdersByItem(
                contract = contract,
                tokenId = tokenId.toString(),
                maker = maker,
                currencyId = currencyId,
                statuses = statuses?.let { it.map { status -> dipDupOrderConverter.convert(status) } } ?: emptyList(),
                platforms = platforms(platforms),
                isBid = false,
                size = size,
                continuation = continuation
            )
        }
        return Slice(
            continuation = page.continuation,
            entities = page.orders.map { dipDupOrderConverter.convert(it, blockchain) }
        )
    }

    override suspend fun getSellOrdersByMaker(
        maker: List<String>,
        platforms: List<TezosPlatform>,
        statuses: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder> {
        val page = safeApiCall {
            dipdupOrderClient.getOrdersByMakers(
                makers = maker,
                statuses = statuses?.let { it.map { status -> dipDupOrderConverter.convert(status) } } ?: emptyList(),
                platforms = platforms(platforms),
                isBid = false,
                size = size,
                continuation = continuation
            )
        }
        return Slice(
            continuation = page.continuation,
            entities = page.orders.map { dipDupOrderConverter.convert(it, blockchain) }
        )
    }

    override suspend fun getSellOrderCurrenciesByItem(contract: String, tokenId: BigInteger): List<UnionAssetType> {
        logger.info("Fetch dipdup sell order currencies by item: $contract, $tokenId")
        return dipDupOrderConverter.convert(
            dipdupOrderClient.getSellOrdersCurrenciesByItem(contract, tokenId.toString()),
            blockchain
        )
    }

    override suspend fun getSellOrderCurrenciesByCollection(contract: String): List<UnionAssetType> {
        logger.info("Fetch dipdup sell order currencies by collection: $contract")
        return dipDupOrderConverter.convert(
            dipdupOrderClient.getSellOrdersCurrenciesByCollection(contract),
            blockchain
        )
    }

    override suspend fun getBidOrdersByItem(
        contract: String,
        tokenId: BigInteger,
        maker: String?,
        platforms: List<TezosPlatform>,
        currencyId: String,
        statuses: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder> {
        logger.info("Fetch dipdup sell orders by item: $contract, $tokenId, $maker, $currencyId, $statuses, $continuation, $size")
        val page = safeApiCall {
            dipdupOrderClient.getOrdersByItem(
                contract = contract,
                tokenId = tokenId.toString(),
                maker = maker,
                currencyId = currencyId,
                statuses = statuses?.let { it.map { status -> dipDupOrderConverter.convert(status) } } ?: emptyList(),
                platforms = platforms(platforms),
                isBid = true,
                size = size,
                continuation = continuation
            )
        }
        return Slice(
            continuation = page.continuation,
            entities = page.orders.map { dipDupOrderConverter.convert(it, blockchain) }
        )
    }

    override suspend fun getBidOrdersByMaker(
        maker: List<String>,
        platforms: List<TezosPlatform>,
        statuses: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<UnionOrder> {
        val page = safeApiCall {
            dipdupOrderClient.getOrdersByMakers(
                makers = maker,
                statuses = statuses?.let { it.map { status -> dipDupOrderConverter.convert(status) } } ?: emptyList(),
                platforms = platforms(platforms),
                isBid = true,
                size = size,
                continuation = continuation
            )
        }
        return Slice(
            continuation = page.continuation,
            entities = page.orders.map { dipDupOrderConverter.convert(it, blockchain) }
        )
    }

    override suspend fun getBidOrderCurrenciesByItem(contract: String, tokenId: BigInteger): List<UnionAssetType> {
        logger.info("Fetch dipdup bid order currencies by item: $contract, $tokenId")
        return dipDupOrderConverter.convert(
            dipdupOrderClient.getBidOrdersCurrenciesByItem(contract, tokenId.toString()),
            blockchain
        )
    }

    override suspend fun getBidOrderCurrenciesByCollection(contract: String): List<UnionAssetType> {
        logger.info("Fetch dipdup bid order currencies by collection: $contract")
        return dipDupOrderConverter.convert(
            dipdupOrderClient.getBidOrdersCurrenciesByCollection(contract),
            blockchain
        )
    }

    private suspend fun <T> safeApiCall(clientCall: suspend () -> T): T {
        return try {
            clientCall()
        } catch (e: DipDupNotFound) {
            throw UnionNotFoundException(message = e.message ?: "")
        } catch (e: WrongArgument) {
            throw UnionValidationException(message = e.message ?: "")
        }
    }

    private fun platforms(requested: Collection<TezosPlatform>) = if (requested.isEmpty()) {
        enabledPlatforms
    } else {
        enabledPlatforms.intersect(requested.toSet()).toList()
    }

    companion object {

        private val logger by Logger()
    }
}
