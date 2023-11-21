package com.rarible.protocol.union.core.service

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.client.CurrencyClient
import com.rarible.protocol.union.core.converter.CurrencyConverter
import com.rarible.protocol.union.core.exception.UnionCurrencyException
import com.rarible.protocol.union.core.model.CurrencyRate
import com.rarible.protocol.union.core.model.UnionAssetType
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CurrencyDto
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
    private val currencyClient: CurrencyClient,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val caches = BlockchainDto.values().associate {
        it to ConcurrentHashMap<String, CurrencyUsdRateDto?>()
    }

    private val currenciesHolder = CurrenciesHolder()

    suspend fun getAllCurrencies(): List<CurrencyDto> {
        return currenciesHolder.getAllCurrencies()
    }

    suspend fun getAllCurrencyRates(): List<CurrencyRate> {
        return currenciesHolder.getAllCurrencyRates()
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
        val cached = blockchainCache[address] ?: refreshCache(blockchain, address)
        val supported = cached.rate >= BigDecimal.ZERO
        return if (supported) cached else null
    }

    private suspend fun refreshCache(blockchain: BlockchainDto, address: String): CurrencyUsdRateDto {
        val blockchainCache = caches[blockchain]!!
        val fetched = fetchRateSafe(blockchain, address, nowMillis())
        if (fetched != null) {
            blockchainCache[address] = fetched
            return fetched
        }

        logger.info("Currency {}:[{}] updated, but doesn't support USD conversion", blockchain.name, address)
        val stub = CurrencyUsdRateDto(
            currencyId = address,
            symbol = "",
            rate = BigDecimal(-1),
            date = nowMillis(),
            abbreviation = null
        )
        blockchainCache[address] = stub
        return stub
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
        val currencyId = assetExt.currencyAddress()

        return toUsd(
            blockchain = blockchain,
            currencyId = currencyId,
            value = value,
            at = at
        )
    }

    suspend fun toUsd(
        blockchain: BlockchainDto,
        assetType: UnionAssetType,
        value: BigDecimal?,
        at: Instant? = null
    ): BigDecimal? {
        if (!assetType.isCurrency() || value == null) {
            return null
        }
        if (value == BigDecimal.ZERO) {
            return BigDecimal.ZERO
        }
        val currencyId = assetType.currencyId()!!

        return toUsd(
            blockchain = blockchain,
            currencyId = currencyId,
            value = value,
            at = at
        )
    }

    private suspend fun toUsd(
        blockchain: BlockchainDto,
        currencyId: String,
        value: BigDecimal,
        at: Instant? = null
    ): BigDecimal? {
        val rate = if (canUseCurrentRate(at)) {
            getCurrentRate(blockchain, currencyId)
        } else {
            fetchRateSafe(blockchain, currencyId, at!!)
        }
        return rate?.let { value.multiply(it.rate) }
    }

    suspend fun getNativeCurrency(blockchain: BlockchainDto): CurrencyDto {
        return currenciesHolder.getNativeCurrencies()[blockchain] ?: throw UnionCurrencyException(
            "Native currency for ${blockchain.name} not found"
        )
    }

    fun invalidateCache() {
        caches.values.forEach { it.clear() }
        currenciesHolder.reset()
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
            currenciesHolder.refresh()
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

    private inner class CurrenciesHolder {

        @Volatile
        private var currencies: List<CurrencyDto> = emptyList()

        @Volatile
        private var nativeCurrencies: Map<BlockchainDto, CurrencyDto> = emptyMap()

        @Volatile
        private var currencyRates: List<CurrencyRate> = emptyList()

        suspend fun getAllCurrencies(): List<CurrencyDto> {
            if (currencies.isEmpty()) {
                refresh()
            }
            return currencies
        }

        suspend fun getNativeCurrencies(): Map<BlockchainDto, CurrencyDto> {
            if (nativeCurrencies.isEmpty()) {
                refresh()
            }
            return nativeCurrencies
        }

        suspend fun getAllCurrencyRates(): List<CurrencyRate> {
            if (currencyRates.isEmpty()) {
                refresh()
            }
            return currencyRates
        }

        suspend fun refresh() {
            currencies = currencyClient.getAllCurrencies()
                .filter { it.blockchain != "OPTIMISM" }
                .map { CurrencyConverter.convert(it) }

            val symbols = BlockchainDto.values().associateWith { getSymbol(it) }
            nativeCurrencies = symbols.mapNotNull { (blockchain, symbol) ->
                val currency = currencies.find {
                    it.currencyId.blockchain == blockchain && it.symbol == symbol
                }
                if (currency != null) blockchain to currency else null
            }.toMap()

            refreshCurrencyRates()
        }

        fun reset() {
            currencies = emptyList()
            nativeCurrencies = emptyMap()
        }

        private fun getSymbol(blockchain: BlockchainDto): String {
            return when (blockchain) {
                BlockchainDto.ETHEREUM -> "ethereum"
                BlockchainDto.POLYGON -> "matic-network"
                BlockchainDto.FLOW -> "flow"
                BlockchainDto.TEZOS -> "tezos"
                BlockchainDto.SOLANA -> "solana"
                BlockchainDto.IMMUTABLEX -> "immutable-x"
                BlockchainDto.MANTLE -> "mantle"
                BlockchainDto.CHILIZ -> "chiliz"
                BlockchainDto.ARBITRUM -> "ethereum"
                BlockchainDto.ZKSYNC -> "ethereum"
                BlockchainDto.ASTARZKEVM -> "ethereum"
                BlockchainDto.BASE -> "ethereum"
                BlockchainDto.LIGHTLINK -> "ethereum"
            }
        }

        private fun refreshCurrencyRates() {
            currencyRates = currencies.mapNotNull { currency ->
                val rate = currency.rate ?: return@mapNotNull null
                CurrencyRate(
                    blockchain = currency.currencyId.blockchain,
                    currencyId = currency.currencyId.fullId(),
                    rate = rate
                )
            }
        }
    }
}
