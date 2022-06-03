package com.rarible.protocol.union.api.service.elastic

import com.ninjasquad.springmockk.MockkBean
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.model.TypedActivityId
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityMint
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityOrderList
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.test.data.randomEsActivity
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant

@IntegrationTest
internal class ActivityElasticServiceIntegrationTest {

    @MockkBean
    private lateinit var router: BlockchainRouter<ActivityService>

    @MockK
    private lateinit var ethereumService: ActivityService

    @MockK
    private lateinit var flowService: ActivityService

    @MockK
    private lateinit var solanaService: ActivityService

    @Autowired
    private lateinit var repository: EsActivityRepository

    @Autowired
    private lateinit var service: ActivityElasticService

    @BeforeEach
    fun setUp() = runBlocking<Unit> {
        every { router.isBlockchainEnabled(BlockchainDto.ETHEREUM) } returns true
        every { router.isBlockchainEnabled(BlockchainDto.FLOW) } returns true
        every { router.isBlockchainEnabled(BlockchainDto.SOLANA) } returns true
        every { router.getService(BlockchainDto.ETHEREUM) } returns ethereumService
        every { router.getService(BlockchainDto.FLOW) } returns flowService
        every { router.getService(BlockchainDto.SOLANA) } returns solanaService

        repository.deleteAll()
        // save some elastic activities
        val one = randomEsActivity().copy(
            activityId = "ETHEREUM:1",
            type = ActivityTypeDto.MINT,
            blockchain = BlockchainDto.ETHEREUM,
            date = Instant.ofEpochMilli(5_000),
            userFrom = "0x112233",
        )
        val two = randomEsActivity().copy(
            activityId = "ETHEREUM:2",
            type = ActivityTypeDto.LIST,
            blockchain = BlockchainDto.ETHEREUM,
            date = Instant.ofEpochMilli(4_900),
            collection = "123",
        )
        val three = randomEsActivity().copy(
            activityId = "FLOW:3",
            type = ActivityTypeDto.MINT,
            blockchain = BlockchainDto.FLOW,
            date = Instant.ofEpochMilli(4_800)
        )
        val four = randomEsActivity().copy(
            activityId = "FLOW:4",
            type = ActivityTypeDto.LIST,
            blockchain = BlockchainDto.FLOW,
            date = Instant.ofEpochMilli(4_700)
        )
        val five = randomEsActivity().copy(
            activityId = "FLOW:5",
            type = ActivityTypeDto.AUCTION_STARTED,
            blockchain = BlockchainDto.FLOW,
            date = Instant.ofEpochMilli(5_700)
        )
        val six = randomEsActivity().copy(
            activityId = "SOLANA:6",
            type = ActivityTypeDto.LIST,
            blockchain = BlockchainDto.SOLANA,
            date = Instant.ofEpochMilli(6_700)
        )
        val seven = randomEsActivity().copy(
            activityId = "ETHEREUM:7",
            type = ActivityTypeDto.AUCTION_STARTED,
            blockchain = BlockchainDto.ETHEREUM,
            date = Instant.ofEpochMilli(4_700),
            item = "222:333",
            userFrom = "0",
            userTo = "0x223344",
        )
        repository.saveAll(listOf(one, two, three, four, five, six, seven).shuffled())
    }

    @Nested
    inner class GetAllActivitiesTest {

        @Test
        fun `should getAllActivities - happy path`() = runBlocking<Unit> {
            // given
            val types = listOf(ActivityTypeDto.MINT, ActivityTypeDto.LIST)
            val blockchains = listOf(BlockchainDto.ETHEREUM, BlockchainDto.FLOW)
            val size = 3
            val sort = ActivitySortDto.LATEST_FIRST

            val eth1 = randomUnionActivityMint(randomEthItemId()).copy(
                id = ActivityIdDto(BlockchainDto.ETHEREUM, "1"),
            )
            val eth2 = randomUnionActivityOrderList(BlockchainDto.ETHEREUM).copy(
                id = ActivityIdDto(BlockchainDto.ETHEREUM, "2"),
            )
            val flow1 = randomUnionActivityMint(randomEthItemId()).copy(
                id = ActivityIdDto(BlockchainDto.FLOW, "3"),
            )

            coEvery {
                ethereumService.getActivitiesByIds(listOf(
                    TypedActivityId("1", ActivityTypeDto.MINT),
                    TypedActivityId("2", ActivityTypeDto.LIST),
                ))
            } returns listOf(eth1, eth2)
            coEvery {
                flowService.getActivitiesByIds(listOf(
                    TypedActivityId("3", ActivityTypeDto.MINT),
                ))
            } returns listOf(flow1)

            // when
            val actual = service.getAllActivities(types, blockchains, null, null, size, sort)

            // then
            assertThat(actual.activities).containsExactly(eth1, eth2, flow1)
            assertThat(actual.cursor).startsWith("4800_")
        }
    }

    @Nested
    inner class GetActivitiesByCollectionTest {

        @Test
        fun `should getActivitiesByCollection - happy path`() = runBlocking<Unit> {
            // given
            val types = listOf(ActivityTypeDto.MINT, ActivityTypeDto.LIST)
            val collection = "ETHEREUM:123"
            val size = 3
            val sort = ActivitySortDto.LATEST_FIRST
            val eth2 = randomUnionActivityOrderList(BlockchainDto.ETHEREUM).copy(
                id = ActivityIdDto(BlockchainDto.ETHEREUM, "2"),
            )
            coEvery {
                ethereumService.getActivitiesByIds(listOf(
                    TypedActivityId("2", ActivityTypeDto.LIST),
                ))
            } returns listOf(eth2)
            // when
            val actual = service.getActivitiesByCollection(types, listOf(collection), null, null, size, sort)

            // then
            assertThat(actual.activities).containsExactly(eth2)
            assertThat(actual.cursor).startsWith("4900_")
        }
    }

    @Nested
    inner class GetActivitiesByItemTest {

        @Test
        fun `should getActivitiesByItem - happy path`() = runBlocking<Unit> {
            // given
            val types = listOf(ActivityTypeDto.MINT, ActivityTypeDto.LIST, ActivityTypeDto.AUCTION_STARTED)
            val item = "ETHEREUM:222:333"
            val size = 3
            val sort = ActivitySortDto.LATEST_FIRST
            val eth7 = randomUnionActivityOrderList(BlockchainDto.ETHEREUM).copy(
                id = ActivityIdDto(BlockchainDto.ETHEREUM, "7"),
            )
            coEvery {
                ethereumService.getActivitiesByIds(listOf(
                    TypedActivityId("7", ActivityTypeDto.AUCTION_STARTED),
                ))
            } returns listOf(eth7)

            // when
            val actual = service.getActivitiesByItem(types, item, null, null, size, sort)

            // then
            assertThat(actual.activities).containsExactly(eth7)
            assertThat(actual.cursor).startsWith("4700_")
        }
    }

    @Nested
    inner class GetActivitiesByUserTest {

        @Test
        fun `should getActivitiesByUser - happy path`() = runBlocking<Unit> {
            // given
            val types = listOf(UserActivityTypeDto.MINT, UserActivityTypeDto.AUCTION_STARTED)
            val blockchains = listOf(BlockchainDto.ETHEREUM)
            val size = 3
            val sort = ActivitySortDto.LATEST_FIRST
            val users = listOf("ETHEREUM:0x112233", "ETHEREUM:0x223344")
            val eth1 = randomUnionActivityOrderList(BlockchainDto.ETHEREUM).copy(
                id = ActivityIdDto(BlockchainDto.ETHEREUM, "1"),
            )
            val eth7 = randomUnionActivityOrderList(BlockchainDto.ETHEREUM).copy(
                id = ActivityIdDto(BlockchainDto.ETHEREUM, "7"),
            )
            coEvery {
                ethereumService.getActivitiesByIds(listOf(
                    TypedActivityId("1", ActivityTypeDto.MINT),
                    TypedActivityId("7", ActivityTypeDto.AUCTION_STARTED),
                ))
            } returns listOf(eth1, eth7)

            // when
            val actual = service.getActivitiesByUser(
                types,
                blockchains = blockchains,
                continuation = null,
                cursor = null,
                size = size,
                sort = sort,
                user = users,
                from = Instant.ofEpochMilli(4_700),
                to = Instant.ofEpochMilli(5_150)
            )

            // then
            assertThat(actual.activities).containsExactly(eth1, eth7)
            assertThat(actual.cursor).startsWith("4700_")
        }
    }
}
