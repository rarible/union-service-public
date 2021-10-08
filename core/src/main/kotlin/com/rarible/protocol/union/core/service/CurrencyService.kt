package com.rarible.protocol.union.core.service

import com.rarible.core.common.nowMillis
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.union.core.converter.CurrencyConverter
import com.rarible.protocol.union.core.exception.UnionCurrencyException
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CurrencyUsdRateDto
import com.rarible.protocol.union.dto.UnionAddress
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
class CurrencyService(
    private val currencyControllerApi: CurrencyControllerApi
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val caches = BlockchainDto.values().associate {
        it to ConcurrentHashMap<String, CurrencyUsdRateDto>()
    }

    // Read currency rate directly from Currency-Service (for API calls)
    suspend fun getRate(blockchain: BlockchainDto, address: String, at: Instant): CurrencyUsdRateDto {
        val result = fetchRateSafe(blockchain, address, at)
        return result ?: throw UnionCurrencyException(
            "Currency for ${blockchain.name} with id [$address] is not supported"
        )
    }

    // Return current rate (cached)
    suspend fun getCurrentRate(address: UnionAddress): CurrencyUsdRateDto {
        return getCurrentRate(address.blockchain, address.value)
    }

    // Return current rate (cached)
    suspend fun getCurrentRate(blockchain: BlockchainDto, address: String): CurrencyUsdRateDto {
        val blockchainCache = caches[blockchain]!!
        var cached = blockchainCache[address]
        if (cached == null) {
            cached = fetchRateSafe(blockchain, address, nowMillis())
            if (cached != null) {
                blockchainCache[address] = cached
            } else {
                throw UnionCurrencyException(
                    "Currency rate for ${blockchain.name} with address [$address] is not supported," +
                            " fix in in currency service"
                )
            }
        }
        return cached
    }

    suspend fun toUsd(assetType: AssetTypeDto, value: BigDecimal?): BigDecimal? {
        if (value == null) {
            return null
        }
        if (value == BigDecimal.ZERO) {
            return BigDecimal.ZERO
        }
        val rate = getCurrentRate(assetType.contract)
        return value.multiply(rate.rate)
    }

    fun invalidateCache() {
        caches.values.forEach { it.clear() }
    }

    @Scheduled(cron = "\${common.currency.refresh.cron:0 0/30 * * * *}")
    fun refreshCache() {
        runBlocking {
            val cachedCurrencyKeys = ArrayList<Pair<BlockchainDto, String>>()
            caches.forEach { e ->
                e.value.forEach {
                    cachedCurrencyKeys.add(e.key to it.key)
                }
            }
            cachedCurrencyKeys.map {
                async { refreshCurrency(it.first, it.second) }
            }.awaitAll()
        }
    }

    private suspend fun refreshCurrency(blockchain: BlockchainDto, address: String) {
        val updated = fetchRateSafe(blockchain, address, nowMillis())
        val blockchainCurrencies = caches[blockchain]!!
        if (updated != null) {
            logger.info(
                "Currency {}:[{}] updated: {} -> {}",
                blockchain.name, address, blockchainCurrencies[address]!!.rate, updated.rate
            )
            blockchainCurrencies[address] = updated
        } else {
            logger.warn(
                "Unable to refresh currency rate for {} with address [{}], will use old value: {}",
                blockchain.name, address, caches[blockchain]!![address]
            )
        }
    }

    private suspend fun fetchRateSafe(blockchain: BlockchainDto, address: String, at: Instant): CurrencyUsdRateDto? {
        return try {
            fetchRate(blockchain, address, at)
        } catch (e: Exception) {
            logger.error("Unable to get currency rate for $blockchain with address [$address]: ${e.message}", e)
            null
        }
    }

    private suspend fun fetchRate(blockchain: BlockchainDto, address: String, at: Instant?): CurrencyUsdRateDto? {
        val result = currencyControllerApi.getCurrencyRate(
            CurrencyConverter.convert(blockchain),
            address,
            (at ?: nowMillis()).toEpochMilli()
        ).awaitFirstOrNull()

        return result?.let { CurrencyConverter.convert(result) }
    }

}