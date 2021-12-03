package com.rarible.protocol.union.core.service

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.client.CurrencyClient
import com.rarible.protocol.union.core.exception.UnionCurrencyException
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CurrencyUsdRateDto
import com.rarible.protocol.union.dto.ext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
class CurrencyService(
    private val currencyClient: CurrencyClient
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val caches = BlockchainDto.values().associate {
        it to ConcurrentHashMap<String, CurrencyUsdRateDto?>()
    }

    // Read currency rate directly from Currency-Service (for API calls)
    suspend fun getRate(blockchain: BlockchainDto, address: String, at: Instant): CurrencyUsdRateDto {
        val result = fetchRateSafe(blockchain, address, at)
        return result ?: throw UnionCurrencyException(
            "Currency for ${blockchain.name} with id [$address] doesn't support USD conversion"
        )
    }

    // Return current rate (cached)
    suspend fun getCurrentRate(blockchain: BlockchainDto, address: String): CurrencyUsdRateDto? {
        val blockchainCache = caches[blockchain]!!
        var cached = blockchainCache[address]
        if (cached == null) {
            cached = fetchRateSafe(blockchain, address, nowMillis())
            if (cached == null) {
                logger.info("Currency {}:[{}] updated, but doesn't support USD conversion", blockchain.name, address)
                cached = CurrencyUsdRateDto(address, "", BigDecimal(-1), nowMillis())
            }
            blockchainCache[address] = cached
        }
        val supported = cached.rate >= BigDecimal.ZERO
        return if (supported) cached else null
    }

    suspend fun toUsd(
        blockchain: BlockchainDto,
        assetType: AssetTypeDto,
        value: BigDecimal?,
        at: Instant? = null
    ): BigDecimal? {
        val assetExt = assetType.ext
        if (!assetExt.isCurrency || value == null) {
            return null
        }
        if (value == BigDecimal.ZERO) {
            return BigDecimal.ZERO
        }
        val address = assetExt.currencyAddress()

        val rate = if (canUseCurrentRate(at)) {
            getCurrentRate(blockchain, address)
        } else {
            fetchRateSafe(blockchain, address, at!!)
        }
        return rate?.let { value.multiply(it.rate) }
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
            currencyClient.fetchRate(blockchain, address, at)
        } catch (e: Exception) {
            logger.error("Unable to get currency rate for $blockchain with address [$address]: ${e.message}", e)
            null
        }
    }

    private fun canUseCurrentRate(at: Instant?): Boolean {
        // We're using current rate for historical rates if date is not greater than 30 minutes ago
        return at == null || System.currentTimeMillis() - at.toEpochMilli() < 30 * 60 * 1000
    }

}
