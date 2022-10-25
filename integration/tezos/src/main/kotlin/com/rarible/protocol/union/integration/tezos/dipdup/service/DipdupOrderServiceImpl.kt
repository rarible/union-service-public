package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.core.logging.Logger
import com.rarible.dipdup.client.OrderClient
import com.rarible.dipdup.client.core.model.TezosPlatform
import com.rarible.dipdup.client.exception.DipDupNotFound
import com.rarible.protocol.union.core.exception.UnionNotFoundException
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OrderDto
import com.rarible.protocol.union.dto.OrderSortDto
import com.rarible.protocol.union.dto.OrderStatusDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupIntegrationProperties
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupOrderConverter
import java.math.BigInteger

class DipdupOrderServiceImpl(
    private val dipdupOrderClient: OrderClient,
    private val dipDupOrderConverter: DipDupOrderConverter,
    private val marketplaces: DipDupIntegrationProperties.Marketplaces
) : DipdupOrderService {

    private val blockchain = BlockchainDto.TEZOS

    fun enabledPlatforms(): List<TezosPlatform> {
        val platforms = listOf(TezosPlatform.RARIBLE_V2, TezosPlatform.RARIBLE_V1).toMutableList()
        if (marketplaces.hen) platforms.add(TezosPlatform.HEN)
        if (marketplaces.objkt) platforms.add(TezosPlatform.OBJKT_V1)
        if (marketplaces.objktV2) platforms.add(TezosPlatform.OBJKT_V2)
        if (marketplaces.versum) platforms.add(TezosPlatform.VERSUM_V1)
        if (marketplaces.teia) platforms.add(TezosPlatform.TEIA_V1)
        if (marketplaces.fxhashV1) platforms.add(TezosPlatform.FXHASH_V1)
        if (marketplaces.fxhashV2) platforms.add(TezosPlatform.FXHASH_V2)
        return platforms
    }

    override suspend fun getOrderById(id: String): OrderDto {
        logger.info("Fetch dipdup order by id: $id")
        val order = safeApiCall { dipdupOrderClient.getOrderById(id) }
        return dipDupOrderConverter.convert(order, blockchain)
    }

    override suspend fun getOrderByIds(ids: List<String>): List<OrderDto> {
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
    ): Slice<OrderDto> {
        logger.info("Fetch dipdup all orders: $sort, $statuses, $continuation, $size")
        val page = dipdupOrderClient.getOrdersAll(
            sort = sort?.let { dipDupOrderConverter.convert(it) },
            statuses = statuses?.let { it.map { status -> dipDupOrderConverter.convert(status) } } ?: emptyList(),
            platforms = enabledPlatforms(),
            isBid = isBid,
            size = size,
            continuation = continuation
        )
        return Slice(
            continuation = page.continuation,
            entities = page.orders.map { dipDupOrderConverter.convert(it, blockchain) }
        )
    }

    override suspend fun getSellOrdersByItem(
        contract: String,
        tokenId: BigInteger,
        maker: String?,
        currencyId: String,
        statuses: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        logger.info("Fetch dipdup sell orders by item: $contract, $tokenId, $maker, $currencyId, $statuses, $continuation, $size")
        val page = dipdupOrderClient.getOrdersByItem(
            contract = contract,
            tokenId = tokenId.toString(),
            maker = maker,
            currencyId = currencyId,
            statuses = statuses?.let { it.map { status -> dipDupOrderConverter.convert(status) } } ?: emptyList(),
            platforms = enabledPlatforms(),
            isBid = false,
            size = size,
            continuation = continuation
        )
        return Slice(
            continuation = page.continuation,
            entities = page.orders.map { dipDupOrderConverter.convert(it, blockchain) }
        )
    }

    override suspend fun getSellOrdersByMaker(
        maker: List<String>,
        statuses: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val page = dipdupOrderClient.getOrdersByMakers(
            makers = maker,
            statuses = statuses?.let { it.map { status -> dipDupOrderConverter.convert(status) } } ?: emptyList(),
            platforms = enabledPlatforms(),
            isBid = false,
            size = size,
            continuation = continuation
        )
        return Slice(
            continuation = page.continuation,
            entities = page.orders.map { dipDupOrderConverter.convert(it, blockchain) }
        )
    }

    override suspend fun getSellOrderCurrenciesByItem(contract: String, tokenId: BigInteger): List<AssetTypeDto> {
        logger.info("Fetch dipdup sell order currencies by item: $contract, $tokenId")
        return dipDupOrderConverter.convert(
            dipdupOrderClient.getSellOrdersCurrenciesByItem(contract, tokenId.toString()),
            blockchain
        )
    }

    override suspend fun getSellOrderCurrenciesByCollection(contract: String): List<AssetTypeDto> {
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
        currencyId: String,
        statuses: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        logger.info("Fetch dipdup sell orders by item: $contract, $tokenId, $maker, $currencyId, $statuses, $continuation, $size")
        val page = dipdupOrderClient.getOrdersByItem(
            contract = contract,
            tokenId = tokenId.toString(),
            maker = maker,
            currencyId = currencyId,
            statuses = statuses?.let { it.map { status -> dipDupOrderConverter.convert(status) } } ?: emptyList(),
            platforms = enabledPlatforms(),
            isBid = true,
            size = size,
            continuation = continuation
        )
        return Slice(
            continuation = page.continuation,
            entities = page.orders.map { dipDupOrderConverter.convert(it, blockchain) }
        )
    }

    override suspend fun getBidOrdersByMaker(
        maker: List<String>,
        statuses: List<OrderStatusDto>?,
        continuation: String?,
        size: Int
    ): Slice<OrderDto> {
        val page = dipdupOrderClient.getOrdersByMakers(
            makers = maker,
            statuses = statuses?.let { it.map { status -> dipDupOrderConverter.convert(status) } } ?: emptyList(),
            platforms = enabledPlatforms(),
            isBid = true,
            size = size,
            continuation = continuation
        )
        return Slice(
            continuation = page.continuation,
            entities = page.orders.map { dipDupOrderConverter.convert(it, blockchain) }
        )
    }

    override suspend fun getBidOrderCurrenciesByItem(contract: String, tokenId: BigInteger): List<AssetTypeDto> {
        logger.info("Fetch dipdup bid order currencies by item: $contract, $tokenId")
        return dipDupOrderConverter.convert(
            dipdupOrderClient.getBidOrdersCurrenciesByItem(contract, tokenId.toString()),
            blockchain
        )
    }

    override suspend fun getBidOrderCurrenciesByCollection(contract: String): List<AssetTypeDto> {
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
        }
    }

    companion object {
        private val logger by Logger()
    }
}


