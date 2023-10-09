package com.rarible.protocol.union.core.converter.helper

import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CurrencyDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.ext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class SellActivityEnricher(
    private val currencyService: CurrencyService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    data class SellVolumeInfo(
        val sellCurrency: String,
        val volumeUsd: Double,
        val volumeSell: Double,
        val volumeNative: Double,
    )

    private val unknownSellVolumeInfo = SellVolumeInfo(
        sellCurrency = "ERROR",
        volumeUsd = 0.0,
        volumeSell = 0.0,
        volumeNative = 0.0
    )

    suspend fun provideVolumeInfo(source: OrderMatchSellDto): SellVolumeInfo {
        try {
            val sellCurrency = extractSellCurrency(source)
            val volumeUsd = extractVolumeUsd(source)

            if (volumeUsd == null) {
                logger.warn(
                    "USD price in OrderMatchSell ${source.id} for Order ${source.orderId}" +
                        "can't be evaluated: $sellCurrency has no rate"
                )
                return unknownSellVolumeInfo
            }

            val volumeNative = extractNativeVolume(
                source = source,
                volumeUsd = volumeUsd,
                sellCurrency = sellCurrency,
                nativeCurrency = currencyService.getNativeCurrency(source.id.blockchain)
            )
            return SellVolumeInfo(
                sellCurrency = sellCurrency,
                volumeUsd = volumeUsd,
                volumeSell = extractSellVolume(source),
                volumeNative = volumeNative
            )
        } catch (e: RuntimeException) {
            logger.error("Error during evaluation of USD price in OrderMatchSell: $source", e)
            return unknownSellVolumeInfo
        }
    }

    private fun extractSellCurrency(source: OrderMatchSellDto): String {
        return source.id.blockchain.name + ":" + source.payment.type.ext.currencyAddress()
    }

    private fun extractSellVolume(source: OrderMatchSellDto): Double {
        return source.payment.value.toDouble()
    }

    private suspend fun extractVolumeUsd(source: OrderMatchSellDto): Double? {
        if (source.amountUsd != null) {
            return source.amountUsd!!.toDouble()
        }

        return currencyService.toUsd(
            blockchain = source.id.blockchain,
            assetType = source.payment.type,
            value = source.payment.value,
            at = source.date
        )?.toDouble()
    }

    private suspend fun extractNativeVolume(
        source: OrderMatchSellDto,
        volumeUsd: Double,
        sellCurrency: String,
        nativeCurrency: CurrencyDto
    ): Double {
        return if (isSellCurrencyNative(sellCurrency, nativeCurrency)) {
            source.payment.value.toDouble()
        } else {
            val nativeRate = currencyService.getRate(
                nativeCurrency.currencyId.blockchain,
                nativeCurrency.currencyId.value,
                source.date
            )
                .rate.toDouble()
            return volumeUsd / nativeRate
        }
    }

    private fun isSellCurrencyNative(sellCurrency: String, nativeCurrency: CurrencyDto): Boolean {
        // edge case for tezos
        if (nativeCurrency.currencyId.blockchain == BlockchainDto.TEZOS && sellCurrency == "TEZOS:XTZ") {
            return true
        }
        return sellCurrency == nativeCurrency.currencyId.toString()
    }
}
