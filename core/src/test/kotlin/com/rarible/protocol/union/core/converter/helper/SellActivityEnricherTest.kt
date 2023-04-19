package com.rarible.protocol.union.core.converter.helper

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.converter.ContractAddressConverter
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.ActivityBlockchainInfoDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.AssetDto
import com.rarible.protocol.union.dto.AssetTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CurrencyDto
import com.rarible.protocol.union.dto.EthErc1155AssetTypeDto
import com.rarible.protocol.union.dto.EthEthereumAssetTypeDto
import com.rarible.protocol.union.dto.OrderActivitySourceDto
import com.rarible.protocol.union.dto.OrderMatchSellDto
import com.rarible.protocol.union.dto.TezosFTAssetTypeDto
import com.rarible.protocol.union.dto.parser.CurrencyIdParser
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import randomUnionAddress
import java.math.BigDecimal
import java.time.Instant

@ExtendWith(MockKExtension::class)
class SellActivityEnricherTest {

    @MockK
    private lateinit var currencyService: CurrencyService

    @InjectMockKs
    private lateinit var enricher: SellActivityEnricher

    @Test
    fun `should provide info when amountUsd is present and sell currency is native`() = runBlocking<Unit> {
        // given
        val usdDecimal = BigDecimal.TEN
        val expectedSellCurrency = "POLYGON:0x0000000000000000000000000000000000000000"
        val expectedVolumeSell = BigDecimal("0.05")
        val source = OrderMatchSellDto(
            id = ActivityIdDto(BlockchainDto.POLYGON, randomString()),
            date = Instant.ofEpochSecond(12345),
            blockchainInfo = ActivityBlockchainInfoDto(
                transactionHash = randomString(),
                blockHash = randomString(),
                blockNumber = randomLong(),
                logIndex = randomInt(),
            ),
            transactionHash = randomString(),
            buyer = randomUnionAddress(),
            seller = randomUnionAddress(),
            nft = AssetDto(
                type = EthErc1155AssetTypeDto(
                    contract = ContractAddressConverter.convert(
                        BlockchainDto.POLYGON,
                        randomAddress().toString(),
                    ),
                    tokenId = 3.toBigInteger()
                ),
                value = BigDecimal("1")
            ),
            payment = AssetDto(
                type = EthEthereumAssetTypeDto(blockchain = BlockchainDto.POLYGON),
                value = expectedVolumeSell
            ),
            source = OrderActivitySourceDto.RARIBLE,
            price = randomBigDecimal(),
            amountUsd = usdDecimal,
            type = OrderMatchSellDto.Type.SELL,
        )
        coEvery {
            currencyService.getNativeCurrency(any())
        } returns CurrencyDto(CurrencyIdParser.parse(expectedSellCurrency), "polygon")

        // when
        val actual = enricher.provideVolumeInfo(source)

        // then
        assertThat(actual.sellCurrency).isEqualTo(expectedSellCurrency)
        assertThat(actual.volumeUsd).isEqualTo(usdDecimal.toDouble())
        assertThat(actual.volumeSell).isEqualTo(expectedVolumeSell.toDouble())
        assertThat(actual.volumeNative).isEqualTo(actual.volumeSell)
        coVerify {
            currencyService.getNativeCurrency(BlockchainDto.POLYGON)
        }
        confirmVerified(currencyService)
    }

    @Test
    fun `should provide info when there is no amountUsd and sell currency is not native`() = runBlocking<Unit> {
        // given
        val usdDecimal = BigDecimal("10.0")
        val expectedSellCurrency = "TEZOS:111222:42"
        val expectedVolumeSell = BigDecimal("0.05")
        val tezosRate = BigDecimal("4.0")
        val expectedVolumeNative = usdDecimal.toDouble() / tezosRate.toDouble()
        val nativeTezosAddress = "tz1Ke2h7sDdakHJQh8WX4Z372du1KChsksyU"
        val source = OrderMatchSellDto(
            id = ActivityIdDto(BlockchainDto.TEZOS, randomString()),
            date = Instant.ofEpochSecond(12345),
            blockchainInfo = ActivityBlockchainInfoDto(
                transactionHash = randomString(),
                blockHash = randomString(),
                blockNumber = randomLong(),
                logIndex = randomInt(),
            ),
            transactionHash = randomString(),
            buyer = randomUnionAddress(),
            seller = randomUnionAddress(),
            nft = AssetDto(
                type = TezosFTAssetTypeDto(
                    contract = ContractAddressConverter.convert(
                        BlockchainDto.TEZOS,
                        randomAddress().toString(),
                    ),
                    tokenId = 3.toBigInteger()
                ),
                value = BigDecimal("1")
            ),
            payment = AssetDto(
                type = TezosFTAssetTypeDto(
                    contract = ContractAddressConverter.convert(
                        BlockchainDto.TEZOS,
                        "111222",
                    ),
                    tokenId = 42.toBigInteger()
                ),
                value = expectedVolumeSell
            ),
            source = OrderActivitySourceDto.RARIBLE,
            price = randomBigDecimal(),
            type = OrderMatchSellDto.Type.SELL,
        )
        coEvery {
            currencyService.getNativeCurrency(any())
        } returns CurrencyDto(CurrencyIdParser.parse("TEZOS:tz1Ke2h7sDdakHJQh8WX4Z372du1KChsksyU"), "tezos")
        coEvery {
            currencyService.toUsd(any(), any<AssetTypeDto>(), any(), any())
        } returns usdDecimal
        coEvery {
            currencyService.getRate(any(), any(), any()).rate
        } returns tezosRate

        // when
        val actual = enricher.provideVolumeInfo(source)

        // then
        assertThat(actual.sellCurrency).isEqualTo(expectedSellCurrency)
        assertThat(actual.volumeUsd).isEqualTo(usdDecimal.toDouble())
        assertThat(actual.volumeSell).isEqualTo(expectedVolumeSell.toDouble())
        assertThat(actual.volumeNative).isEqualTo(expectedVolumeNative)
        coVerify {
            currencyService.getNativeCurrency(BlockchainDto.TEZOS)
            currencyService.toUsd(BlockchainDto.TEZOS, source.payment.type, source.payment.value, source.date)
            currencyService.getRate(BlockchainDto.TEZOS, nativeTezosAddress, source.date)
        }
        confirmVerified(currencyService)
    }

    @Test
    fun `should return empty info on error`() = runBlocking<Unit> {
        // given
        coEvery {
            currencyService.getNativeCurrency(any())
        } throws RuntimeException("This is expetcted error")

        // when
        val actual = enricher.provideVolumeInfo(mockk())

        // then
        assertThat(actual.sellCurrency).isEqualTo("ERROR")
        assertThat(actual.volumeSell).isZero()
        assertThat(actual.volumeUsd).isZero()
        assertThat(actual.volumeNative).isZero()
    }
}