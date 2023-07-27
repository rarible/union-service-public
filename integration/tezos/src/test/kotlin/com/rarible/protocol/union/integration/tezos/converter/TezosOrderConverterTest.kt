package com.rarible.protocol.union.integration.tezos.converter

import com.rarible.dipdup.client.core.model.Asset
import com.rarible.dipdup.client.core.model.DipDupOrder
import com.rarible.dipdup.client.core.model.OrderStatus
import com.rarible.dipdup.client.core.model.TezosPlatform
import com.rarible.protocol.union.core.model.UnionTezosNFTAssetType
import com.rarible.protocol.union.core.model.UnionTezosXTZAssetType
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.PlatformDto
import com.rarible.protocol.union.dto.TezosOrderDataRaribleV2DataV2Dto
import com.rarible.protocol.union.integration.tezos.data.randomTezosOrderDto
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupOrderConverter
import com.rarible.protocol.union.test.mock.CurrencyMock
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.OffsetDateTime

class TezosOrderConverterTest {

    private val tezosOrderConverter = DipDupOrderConverter(CurrencyMock.currencyServiceMock)

    @Test
    fun `tezos order`() = runBlocking<Unit> {
        val dto = randomTezosOrderDto()

        val converted = tezosOrderConverter.convert(dto, BlockchainDto.TEZOS)

        assertThat(converted.id.value).isEqualTo(dto.id)
        assertThat(converted.platform).isEqualTo(PlatformDto.RARIBLE)
        assertThat(converted.maker.value).isEqualTo(dto.maker)
        assertThat(converted.make.type).isInstanceOf(UnionTezosNFTAssetType::class.java)
        assertThat(converted.make.value).isEqualTo(dto.make.assetValue)
        assertThat(converted.take.type).isInstanceOf(UnionTezosXTZAssetType::class.java)
        assertThat(converted.take.value).isEqualTo(dto.take.assetValue)
        assertThat(converted.salt).isEqualTo(dto.salt.toString())
        assertThat(converted.fill).isEqualTo(dto.fill)
        assertThat(converted.startedAt).isEqualTo(dto.startAt?.toInstant())
        assertThat(converted.endedAt).isEqualTo(dto.endAt?.toInstant())
        assertThat(converted.cancelled).isEqualTo(dto.cancelled)
        assertThat(converted.createdAt).isEqualTo(dto.createdAt.toInstant())
        assertThat(converted.lastUpdatedAt).isEqualTo(dto.lastUpdatedAt.toInstant())
        assertThat(converted.takePrice).isNull()
        assertThat(converted.makePrice).isEqualTo(converted.take.value.setScale(18) / converted.make.value.setScale(18))
        assertThat(converted.takePriceUsd).isNull()
        // In mock we are converting price 1 to 1
        assertThat(converted.makePriceUsd).isEqualTo(dto.take.assetValue.setScale(18) / dto.make.assetValue.setScale(18))

        assertThat(converted.data).isInstanceOf(TezosOrderDataRaribleV2DataV2Dto::class.java)
    }

    @Test
    fun `tezos should take makePrice`() = runBlocking<Unit> {
        val dto = DipDupOrder(
            id = "fde834ac-63cd-576a-9a30-0832fa493799",
            internalOrderId = "2923516",
            fill = BigDecimal.ZERO,
            platform = TezosPlatform.OBJKT_V2,
            cancelled = false,
            status = OrderStatus.ACTIVE,
            startAt = OffsetDateTime.now(),
            endedAt = null,
            endAt = null,
            createdAt = OffsetDateTime.now(),
            lastUpdatedAt = OffsetDateTime.now(),
            maker = "tz1btzFK9Gg4nv4kEQNEpV5nnUXW6gHKDDNo",
            make = Asset(
                assetType = Asset.MT(
                    assetClass = "TEZOS_MT",
                    contract = "KT1RJ6PbjHpwc3M5rw5s2Nbmefwbuwbdxton",
                    tokenId = 603126.toBigInteger()
                ),
                assetValue = BigDecimal.ONE
            ),
            makePrice = 3.3.toBigDecimal(),
            taker = null,
            take = Asset(assetType = Asset.XTZ(assetClass = "XTZ"), assetValue = BigDecimal.ZERO),
            takePrice = 3.3.toBigDecimal(),
            salt = 27871898.toBigInteger(),
            originFees = emptyList(),
            payouts = emptyList(),
            legacyData = null,
            makeStock = null
        )

        val converted = tezosOrderConverter.convert(dto, BlockchainDto.TEZOS)
        assertThat(converted.makePrice).isEqualTo(3.3.toBigDecimal())
        assertThat(converted.takePrice).isNull()

        // if makeStock is missed we use "old" way
        assertThat(converted.makeStock).isEqualTo(BigDecimal.ONE)
    }

    @Test
    fun `tezos objkt should calculate makePrice`() = runBlocking<Unit> {
        val dto = DipDupOrder(
            id = "fde834ac-63cd-576a-9a30-0832fa493799",
            internalOrderId = "2923516",
            fill = BigDecimal.ZERO,
            platform = TezosPlatform.OBJKT_V2,
            cancelled = false,
            status = OrderStatus.ACTIVE,
            startAt = OffsetDateTime.now(),
            endedAt = null,
            endAt = null,
            createdAt = OffsetDateTime.now(),
            lastUpdatedAt = OffsetDateTime.now(),
            maker = "tz1btzFK9Gg4nv4kEQNEpV5nnUXW6gHKDDNo",
            make = Asset(
                assetType = Asset.MT(
                    assetClass = "TEZOS_MT",
                    contract = "KT1RJ6PbjHpwc3M5rw5s2Nbmefwbuwbdxton",
                    tokenId = 603126.toBigInteger()
                ),
                assetValue = 1.toBigDecimal()
            ),
            makeStock = 1.toBigDecimal(),
            makePrice = null,
            taker = null,
            take = Asset(assetType = Asset.XTZ(assetClass = "XTZ"), assetValue = 0.1.toBigDecimal()),
            takePrice = null,
            salt = 27871898.toBigInteger(),
            originFees = emptyList(),
            payouts = emptyList(),
            legacyData = null
        )

        val converted = tezosOrderConverter.convert(dto, BlockchainDto.TEZOS)
        assertThat(converted.makePrice).isEqualTo(0.1.toBigDecimal().setScale(18))
        assertThat(converted.takePrice).isNull()
        assertThat(converted.makeStock).isEqualTo(1.toBigDecimal())
    }
}
