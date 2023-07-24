package com.rarible.protocol.union.enrichment.custom.collection.updater

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.FeatureFlagsProperties
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivityIdDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.converter.ActivityDtoConverter
import com.rarible.protocol.union.enrichment.converter.EnrichmentActivityConverter
import com.rarible.protocol.union.enrichment.repository.ActivityRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentActivityService
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityMint
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class CustomCollectionActivityUpdaterTest {

    @MockK
    lateinit var activityService: ActivityService

    @MockK
    lateinit var router: BlockchainRouter<ActivityService>

    @MockK
    lateinit var producer: RaribleKafkaProducer<ActivityDto>

    @MockK
    lateinit var activityRepository: ActivityRepository

    @MockK
    lateinit var enrichmentActivityService: EnrichmentActivityService

    lateinit var updater: CustomCollectionActivityUpdater

    @BeforeEach
    fun beforeEach() {
        clearMocks(router, enrichmentActivityService, activityService)
        every { router.getService(BlockchainDto.ETHEREUM) } returns activityService
        coEvery { enrichmentActivityService.enrich(emptyList()) } returns emptyList()
        coEvery { producer.send(any<Collection<KafkaMessage<ActivityDto>>>()) } returns emptyFlow()
    }

    @Test
    fun `update - ok deprecated`() = runBlocking<Unit> {
        updater = CustomCollectionActivityUpdater(
            router = router,
            enrichmentActivityService = enrichmentActivityService,
            eventProducer = producer,
            activityRepository = activityRepository,
            featureFlagsProperties = FeatureFlagsProperties()
        )
        val itemId = randomEthItemId()
        val item = randomUnionItem(itemId)
        val activity1 = randomUnionActivityMint(itemId)
        val activity2 = randomUnionActivityMint(itemId).copy(reverted = true)
        val activity3 = randomUnionActivityMint(itemId)

        mockkGetActivities(itemId, null, "1", activity1, activity2)
        mockkGetActivities(itemId, "1", "2", activity3)
        mockkGetActivities(itemId, "2", null)

        val dto1 = ActivityDtoConverter.convert(activity1)
        val dto3 = ActivityDtoConverter.convert(activity3)

        coEvery { enrichmentActivityService.enrichDeprecated(listOf(activity1)) } returns listOf(dto1)
        coEvery { enrichmentActivityService.enrichDeprecated(listOf(activity3)) } returns listOf(dto3)
        coEvery { enrichmentActivityService.enrichDeprecated(emptyList()) } returns emptyList()

        updater.update(item)

        assertEvents(listOf(activity1.id))
        assertEvents(listOf(activity3.id))
    }

    @Test
    fun `update - ok`() = runBlocking<Unit> {
        updater = CustomCollectionActivityUpdater(
            router = router,
            enrichmentActivityService = enrichmentActivityService,
            eventProducer = producer,
            activityRepository = activityRepository,
            featureFlagsProperties = FeatureFlagsProperties(enableMongoActivityWrite = true)
        )
        val itemId = randomEthItemId()
        val item = randomUnionItem(itemId)
        val activity1 = randomUnionActivityMint(itemId)
        val activity2 = randomUnionActivityMint(itemId).copy(reverted = true)
        val activity3 = randomUnionActivityMint(itemId)

        mockkGetActivities(itemId, null, "1", activity1, activity2)
        mockkGetActivities(itemId, "1", "2", activity3)
        mockkGetActivities(itemId, "2", null)

        val enrichment1 = EnrichmentActivityConverter.convert(activity1)
        val enrichment3 = EnrichmentActivityConverter.convert(activity3)

        coEvery { activityRepository.save(any()) } returnsArgument 0
        coEvery { enrichmentActivityService.enrich(listOf(activity1)) } returns listOf(enrichment1)
        coEvery { enrichmentActivityService.enrich(listOf(activity3)) } returns listOf(enrichment3)

        updater.update(item)

        assertEvents(listOf(activity1.id))
        assertEvents(listOf(activity3.id))
    }

    private fun assertEvents(
        expected: List<ActivityIdDto>
    ) {
        coVerify {
            producer.send(match<Collection<KafkaMessage<ActivityDto>>> { events ->
                val received = events.map { it.value.id }
                assertThat(received).isEqualTo(expected)
                true
            })
        }
    }

    private fun mockkGetActivities(
        itemId: ItemIdDto,
        continuation: String? = null,
        returnContinuation: String? = null,
        vararg activities: UnionActivity
    ) {
        val activityTypes = listOf(
            ActivityTypeDto.TRANSFER,
            ActivityTypeDto.MINT,
            ActivityTypeDto.BURN,
            ActivityTypeDto.BID,
            ActivityTypeDto.LIST,
            ActivityTypeDto.SELL,
            ActivityTypeDto.CANCEL_LIST,
            ActivityTypeDto.CANCEL_BID
        )

        coEvery {
            activityService.getActivitiesByItem(
                types = activityTypes,
                itemId = itemId.value,
                continuation = continuation,
                size = any(),
                sort = ActivitySortDto.EARLIEST_FIRST
            )
        } returns Slice(returnContinuation, activities.toList())
    }
}