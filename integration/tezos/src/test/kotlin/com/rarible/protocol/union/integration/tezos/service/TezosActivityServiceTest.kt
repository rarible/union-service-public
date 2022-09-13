package com.rarible.protocol.union.integration.tezos.service

import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomString
import com.rarible.dipdup.client.OrderActivityClient
import com.rarible.dipdup.client.model.DipDupActivitiesPage
import com.rarible.dipdup.client.model.DipDupActivityType
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.integration.tezos.data.randomDipDupActivityOrderListEvent
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
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

class TezosActivityServiceTest {

    private val currencyService: CurrencyService = CurrencyMock.currencyServiceMock
    private val testDipDupOrderActivityClient: OrderActivityClient = mockk()
    private val dipdupOrderActivityService: DipdupOrderActivityService = DipdupOrderActivityServiceImpl(
        testDipDupOrderActivityClient, DipDupActivityConverter(currencyService)
    )
    private val tzktTokenClient: TokenActivityClient = mockk()
    private val tzktItemActivityService: TzktItemActivityService = TzktItemActivityServiceImpl(tzktTokenClient)

    private val service = TezosActivityService(
        dipdupOrderActivityService,
        tzktItemActivityService
    )

    @Test
    fun `should return all dipdup,tzkt activities`() = runBlocking<Unit> {

        coEvery {
            testDipDupOrderActivityClient.getActivitiesAll(
                listOf(DipDupActivityType.LIST),
                10,
                null,
                false
            )
        } returns DipDupActivitiesPage(
            continuation = null,
            activities = listOf(randomDipDupActivityOrderListEvent(randomString()))
        )
        coEvery { tzktTokenClient.getActivitiesAll(listOf(ActivityType.MINT), 10, null, false) } returns Page(
            continuation = null,
            items = listOf(randomTzktItemMintActivity(randomInt().toString()))
        )

        val activities = service.getAllActivities(listOf(ActivityTypeDto.MINT, ActivityTypeDto.LIST), null, 10, null)
        Assertions.assertThat(activities.entities).hasSize(2)
    }

}
