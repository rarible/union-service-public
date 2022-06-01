package com.rarible.protocol.union.integration.tezos.service

import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomString
import com.rarible.dipdup.client.OrderActivityClient
import com.rarible.dipdup.client.model.DipDupActivitiesPage
import com.rarible.dipdup.client.model.DipDupActivityType
import com.rarible.protocol.tezos.api.client.NftActivityControllerApi
import com.rarible.protocol.tezos.api.client.OrderActivityControllerApi
import com.rarible.protocol.tezos.dto.OrderActivitiesDto
import com.rarible.protocol.union.core.model.TypedActivityId
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.integration.tezos.converter.TezosActivityConverter
import com.rarible.protocol.union.integration.tezos.data.randomDipDupActivityOrderListEvent
import com.rarible.protocol.union.integration.tezos.data.randomTezosOrderListActivity
import com.rarible.protocol.union.integration.tezos.data.randomTzktItemMintActivity
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupActivityConverter
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipdupOrderActivityService
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipdupOrderActivityServiceImpl
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemActivityService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemActivityServiceImpl
import com.rarible.protocol.union.test.mock.CurrencyMock
import com.rarible.tzkt.client.TokenActivityClient
import com.rarible.tzkt.model.ActivityType
import com.rarible.tzkt.model.Page
import io.mockk.coEvery
import io.mockk.mockk
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toMono

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
    private val tzktTokenClient: TokenActivityClient = mockk()
    private val tzktItemActivityService: TzktItemActivityService = TzktItemActivityServiceImpl(tzktTokenClient)

    private val service = TezosActivityService(
        activityItemControllerApi,
        activityOrderControllerApi,
        tezosActivityConverter,
        pgService,
        dipdupOrderActivityService,
        tzktItemActivityService
    )

    @Test
    fun `should get legacy activity plus dipdup,tzkt activitis by ids`() = runBlocking<Unit> {
        val legacyActivity = TypedActivityId(
            id = "BKpJX4yv2JsxezPcvgnavyjJZBZVbQ5hicMwQLEkxv9516Qz27N_46",
            type = ActivityTypeDto.LIST
        )
        val orderActivity = TypedActivityId(
            id = UUID.randomUUID().toString(),
            type = ActivityTypeDto.LIST
        )
        val itemActivity = TypedActivityId(
            id = randomInt().toString(),
            type = ActivityTypeDto.MINT
        )

        coEvery { pgService.orderActivities(listOf(legacyActivity.id)) } returns OrderActivitiesDto(
            items = listOf(randomTezosOrderListActivity()),
            continuation = null
        )
        coEvery { testDipDupOrderActivityClient.getActivitiesByIds(listOf(orderActivity.id)) } returns listOf(
            randomDipDupActivityOrderListEvent(orderActivity.id)
        )
        coEvery { tzktTokenClient.getActivitiesByIds(listOf(itemActivity.id)) } returns listOf(
            randomTzktItemMintActivity(itemActivity.id)
        )
        val types = listOf(
            legacyActivity,
            orderActivity,
            itemActivity
        )
        val activities = service.getActivitiesByIds(types)
        Assertions.assertThat(activities).hasSize(3)
    }

    @Test
    fun `should return all dipdup,tzkt activities`() = runBlocking<Unit> {

        coEvery { activityOrderControllerApi.getOrderActivities(any(), 10, any(), any()) } returns OrderActivitiesDto(
            continuation = null,
            items = listOf(randomTezosOrderListActivity())
        ).toMono()
        coEvery {
            testDipDupOrderActivityClient.getActivitiesAll(
                listOf(DipDupActivityType.LIST),
                9,
                null,
                false
            )
        } returns DipDupActivitiesPage(
            continuation = null,
            activities = listOf(randomDipDupActivityOrderListEvent(randomString()))
        )
        coEvery { tzktTokenClient.getActivitiesAll(listOf(ActivityType.MINT), 9, null, false) } returns Page(
            continuation = null,
            items = listOf(randomTzktItemMintActivity(randomInt().toString()))
        )

        val activities = service.getAllActivities(listOf(ActivityTypeDto.MINT, ActivityTypeDto.LIST), null, 10, null)
        Assertions.assertThat(activities.entities).hasSize(3)
    }

}
