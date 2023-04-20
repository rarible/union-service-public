package com.rarible.protocol.union.worker.job.collection

import com.rarible.protocol.union.core.converter.EsActivityConverter
import com.rarible.protocol.union.core.model.EsActivity
import com.rarible.protocol.union.core.model.UnionActivityDto
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.converter.ActivityDtoConverter
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentActivityService
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityMint
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.elasticsearch.action.support.WriteRequest
import org.junit.jupiter.api.Assertions.*
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
    lateinit var repository: EsActivityRepository

    @MockK
    lateinit var converter: EsActivityConverter

    @MockK
    lateinit var enrichmentActivityService: EnrichmentActivityService

    @InjectMockKs
    lateinit var updater: CustomCollectionActivityUpdater

    @BeforeEach
    fun beforeEach() {
        clearMocks(router, repository, enrichmentActivityService, activityService, converter)
        every { router.getService(BlockchainDto.ETHEREUM) } returns activityService
        coEvery { repository.bulk(any(), any(), any(), any()) } returns Unit
        coEvery { enrichmentActivityService.enrich(emptyList()) } returns emptyList()
        coEvery { converter.batchConvert(emptyList()) } returns emptyList()
    }

    @Test
    fun `update - ok`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val item = randomUnionItem(itemId)
        val activity1 = randomUnionActivityMint(itemId)
        val activity2 = randomUnionActivityMint(itemId).copy(reverted = true)

        mockkGetActivities(itemId, null, "1", activity1)
        mockkGetActivities(itemId, "1", "2", activity2)
        mockkGetActivities(itemId, "2", null)

        val dto = ActivityDtoConverter.convert(activity1)
        val esDto = mockk<EsActivity>()

        coEvery { enrichmentActivityService.enrich(listOf(activity1)) } returns listOf(dto)
        coEvery { converter.batchConvert(listOf(dto)) } returns listOf(esDto)

        updater.update(item)

        coVerify { repository.bulk(listOf(esDto), emptyList(), null, WriteRequest.RefreshPolicy.NONE) }
    }

    private fun mockkGetActivities(
        itemId: ItemIdDto,
        continuation: String? = null,
        returnContinuation: String? = null,
        vararg activities: UnionActivityDto
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