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

    suspend fun provideVolumeInfo(source: OrderMatchSellDto): SellVolumeInfo {
        try {
            val nativeCurrency = currencyService.getNativeCurrency(source.id.blockchain)
            val sellCurrency = extractSellCurrency(source)
            val volumeUsd = extractVolumeUsd(source)
            val volumeSell = extractVolumeSell(source)
            val volumeNative = extractVolumeNative(source, volumeUsd, sellCurrency, nativeCurrency)
            return SellVolumeInfo(
                sellCurrency,
                volumeUsd,
                volumeSell,
                volumeNative
            )
        } catch (e: RuntimeException) {
            logger.error("Error while enriching sell activity $source", e)
            return SellVolumeInfo(
                sellCurrency = "ERROR",
                volumeUsd = 0.0,
                volumeSell = 0.0,
                volumeNative = 0.0
            )
        }
    }

    private fun extractSellCurrency(source: OrderMatchSellDto): String {
        return source.id.blockchain.name + ":" + source.payment.type.ext.currencyAddress()
    }

    private suspend fun extractVolumeUsd(source: OrderMatchSellDto): Double {
        return if (source.amountUsd != null) {
            source.amountUsd!!.toDouble()
        } else {
            currencyService.toUsd(source.id.blockchain, source.payment.type, source.payment.value, source.date)!!
                .toDouble()
        }
    }

    private fun extractVolumeSell(source: OrderMatchSellDto): Double {
        return source.payment.value.toDouble()
    }

    private suspend fun extractVolumeNative(
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
