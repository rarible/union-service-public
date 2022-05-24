package com.rarible.protocol.union.integration.tezos.service

import com.rarible.dipdup.client.OrderActivityClient
import com.rarible.dipdup.client.core.model.Asset
import com.rarible.dipdup.client.core.model.DipDupActivity
import com.rarible.dipdup.client.core.model.DipDupOrderListActivity
import com.rarible.dipdup.client.core.model.TezosPlatform
import com.rarible.protocol.tezos.api.client.NftActivityControllerApi
import com.rarible.protocol.tezos.api.client.OrderActivityControllerApi
import com.rarible.protocol.tezos.dto.OrderActivitiesDto
import com.rarible.protocol.union.core.model.TypedActivityId
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.integration.tezos.converter.TezosActivityConverter
import com.rarible.protocol.union.integration.tezos.data.randomTezosOrderActivityCancelList
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupActivityConverter
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipdupOrderActivityService
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipdupOrderActivityServiceImpl
import com.rarible.protocol.union.test.mock.CurrencyMock
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.ZoneOffset
import java.util.*

class TezosActivityServiceTest {

    private val activityItemControllerApi: NftActivityControllerApi = mockk()
    private val activityOrderControllerApi: OrderActivityControllerApi = mockk()
    private val currencyService: CurrencyService = CurrencyMock.currencyServiceMock
    private val tezosActivityConverter: TezosActivityConverter = TezosActivityConverter(currencyService)
    private val pgService: TezosPgActivityService = mockk()
    private val testDipDupOrderActivityClient: OrderActivityClient = mockk()
    private val dipdupOrderActivityService: DipdupOrderActivityService = DipdupOrderActivityServiceImpl(
        testDipDupOrderActivityClient, DipDupActivityConverter(currencyService)
    )

    private val service = TezosActivityService(
        activityItemControllerApi,
        activityOrderControllerApi,
        tezosActivityConverter,
        pgService,
        dipdupOrderActivityService
    )

    @Test
    fun `should get legacy activity + dipdup activity`() = runBlocking<Unit> {
        val legacyActivity = TypedActivityId(
            id = "BKpJX4yv2JsxezPcvgnavyjJZBZVbQ5hicMwQLEkxv9516Qz27N_46",
            type = ActivityTypeDto.LIST
        )
        val activity = TypedActivityId(
            id = UUID.randomUUID().toString(),
            type = ActivityTypeDto.LIST
        )

        coEvery { pgService.orderActivities(listOf(legacyActivity.id)) } returns OrderActivitiesDto(
            items = listOf(randomTezosOrderActivityCancelList()),
            continuation = null
        )
        coEvery { testDipDupOrderActivityClient.getActivities(listOf(activity.id)) } returns listOf(
            randomDipDupActivityOrderListEvent(activity.id)
        )
        val types = listOf(
            legacyActivity,
            activity
        )
        val activities = service.getActivitiesByIds(types)
        Assertions.assertThat(activities).hasSize(2)
    }

    private fun randomDipDupActivityOrderListEvent(activityId: String): DipDupActivity {
        return DipDupOrderListActivity(
            id = activityId,
            date = Instant.now().atOffset(ZoneOffset.UTC),
            reverted = false,
            hash = "",
            maker = UUID.randomUUID().toString(),
            make = Asset(
                assetType = Asset.NFT(
                    contract = UUID.randomUUID().toString(),
                    tokenId = BigInteger.ONE
                ),
                assetValue = BigDecimal.ONE
            ),
            take = Asset(
                assetType = Asset.XTZ(),
                assetValue = BigDecimal.ONE
            ),
            price = BigDecimal.ONE,
            source = TezosPlatform.Rarible
        )
    }

}
