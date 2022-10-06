package com.rarible.protocol.union.integration.tezos.service

import com.rarible.core.test.data.randomInt
import com.rarible.core.test.data.randomLong
import com.rarible.core.test.data.randomString
import com.rarible.dipdup.client.OrderActivityClient
import com.rarible.dipdup.client.TokenActivityClient
import com.rarible.dipdup.client.model.DipDupActivitiesPage
import com.rarible.dipdup.client.model.DipDupActivityType
import com.rarible.protocol.union.core.service.CurrencyService
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.integration.tezos.data.randomDipDupActivityMint
import com.rarible.protocol.union.integration.tezos.data.randomDipDupActivityOrderListEvent
import com.rarible.protocol.union.integration.tezos.data.randomTzktItemMintActivity
import com.rarible.protocol.union.integration.tezos.dipdup.DipDupIntegrationProperties
import com.rarible.protocol.union.integration.tezos.dipdup.converter.DipDupActivityConverter
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipDupTokenActivityService
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipdupOrderActivityService
import com.rarible.protocol.union.integration.tezos.dipdup.service.DipdupOrderActivityServiceImpl
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemActivityService
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktItemActivityServiceImpl
import com.rarible.protocol.union.test.mock.CurrencyMock
import com.rarible.tzkt.model.ActivityType
import com.rarible.tzkt.model.Page
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TezosActivityServiceTest {

    private val currencyService: CurrencyService = CurrencyMock.currencyServiceMock
    private val testDipDupOrderActivityClient: OrderActivityClient = mockk()
    private val testDipDupTokenActivityClient: TokenActivityClient = mockk()
    private val converter = DipDupActivityConverter(currencyService)
    private val dipdupOrderActivityService: DipdupOrderActivityService = DipdupOrderActivityServiceImpl(
        testDipDupOrderActivityClient, converter
    )
    private val dipDupTokenActivityService: DipDupTokenActivityService = DipDupTokenActivityService(
        testDipDupTokenActivityClient, converter
    )
    private val tzktTokenClient: com.rarible.tzkt.client.TokenActivityClient = mockk()
    private val tzktItemActivityService: TzktItemActivityService = TzktItemActivityServiceImpl(tzktTokenClient)
    private val dipdupProps: DipDupIntegrationProperties = mockk()
    private val tzktProps: DipDupIntegrationProperties.TzktProperties = mockk()

    private val service = TezosActivityService(
        dipdupOrderActivityService,
        dipDupTokenActivityService,
        tzktItemActivityService,
        dipdupProps
    )

    @BeforeEach
    fun beforeEach() {
        every { dipdupProps.tzktProperties } returns tzktProps
        every { tzktProps.wrapActivityHashes } returns false
    }

    @Test
    fun `should return all dipdup, tzkt activities`() = runBlocking<Unit> {
        coEvery { dipdupProps.useDipDupTokens } returns false
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

    @Test
    fun `should return only dipdup activities`() = runBlocking<Unit> {
        coEvery { dipdupProps.useDipDupTokens } returns true
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
        coEvery { testDipDupTokenActivityClient.getActivitiesAll(listOf(DipDupActivityType.MINT), 10, null, false) } returns DipDupActivitiesPage(
            continuation = null,
            activities = listOf(randomDipDupActivityMint(randomLong().toString()))
        )

        val activities = service.getAllActivities(listOf(ActivityTypeDto.MINT, ActivityTypeDto.LIST), null, 10, null)
        Assertions.assertThat(activities.entities).hasSize(2)
    }
}
