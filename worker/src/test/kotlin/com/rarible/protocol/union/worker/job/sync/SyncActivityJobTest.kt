package com.rarible.protocol.union.worker.job.sync

import com.rarible.core.common.nowMillis
import com.rarible.protocol.union.core.converter.EsActivityConverter
import com.rarible.protocol.union.core.event.OutgoingActivityEventListener
import com.rarible.protocol.union.core.model.UnionActivity
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.SyncTypeDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.converter.EnrichmentActivityConverter
import com.rarible.protocol.union.enrichment.converter.EnrichmentActivityDtoConverter
import com.rarible.protocol.union.enrichment.repository.search.EsActivityRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentActivityService
import com.rarible.protocol.union.enrichment.test.data.randomEsActivity
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityBurn
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityTransfer
import com.rarible.protocol.union.worker.task.search.EsRateLimiter
import io.mockk.Called
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.elasticsearch.action.support.WriteRequest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SyncActivityJobTest {

    @MockK
    lateinit var enrichmentActivityService: EnrichmentActivityService

    @MockK
    lateinit var activityService: ActivityService

    @MockK
    lateinit var activityServiceRouter: BlockchainRouter<ActivityService>

    @MockK
    lateinit var esActivityRepository: EsActivityRepository

    @MockK
    lateinit var esActivityConverter: EsActivityConverter

    @MockK
    lateinit var esRateLimiter: EsRateLimiter

    @MockK
    lateinit var outgoingActivityEventListener: OutgoingActivityEventListener

    @InjectMockKs
    lateinit var job: SyncActivityJob

    @BeforeEach
    @Suppress("UNCHECKED_CAST")
    fun beforeEach() {
        clearMocks(activityService, activityServiceRouter, esRateLimiter, outgoingActivityEventListener)
        coEvery { esRateLimiter.waitIfNecessary(any()) } returns Unit
        every { activityServiceRouter.getService(BlockchainDto.ETHEREUM) } returns activityService
        coEvery { esActivityRepository.bulk(any(), any(), any(), WriteRequest.RefreshPolicy.NONE) } returns Unit
        coEvery { enrichmentActivityService.update(any()) } answers {
            EnrichmentActivityConverter.convert(it.invocation.args[0] as UnionActivity)
        }
        coEvery { esActivityConverter.batchConvert(any()) } answers {
            (it.invocation.args[0] as List<ActivityDto>).map { dto ->
                randomEsActivity().copy(activityId = dto.id.fullId())
            }
        }
        coEvery { outgoingActivityEventListener.onEvents(any()) } returns Unit
    }

    @Test
    fun `activities synced - db and es`() = runBlocking<Unit> {
        val reverted = randomUnionActivityBurn().copy(reverted = true)
        val live = randomUnionActivityTransfer()

        val param = """{
            "blockchain": "ETHEREUM",
            "scope": "ES", 
            "sort": "DB_UPDATE_ASC", 
            "type": "NFT"
        }"""

        coEvery {
            activityService.getAllActivitiesSync(null, any(), SyncSortDto.DB_UPDATE_ASC, SyncTypeDto.NFT)
        } returns Slice(null, listOf(reverted, live))

        job.handle(null, param).toList()

        coVerify(exactly = 1) { enrichmentActivityService.update(reverted) }
        coVerify(exactly = 1) { enrichmentActivityService.update(live) }
        coVerify { outgoingActivityEventListener wasNot Called }
        coVerify(exactly = 1) {
            esActivityRepository.bulk(
                match { batch -> batch.map { it.activityId } == listOf(live.id.fullId()) },
                listOf(reverted.id.fullId()),
                null,
                WriteRequest.RefreshPolicy.NONE
            )
        }
    }

    @Test
    fun `activities synced - event`() = runBlocking<Unit> {
        val reverted = randomUnionActivityBurn().copy(reverted = true)
        val live = randomUnionActivityTransfer()

        val param = """{
            "blockchain": "ETHEREUM",
            "scope": "EVENT", 
            "sort": "DB_UPDATE_ASC", 
            "type": "NFT"
        }"""

        coEvery {
            activityService.getAllActivitiesSync(null, any(), SyncSortDto.DB_UPDATE_ASC, SyncTypeDto.NFT)
        } returns Slice(null, listOf(reverted, live))

        job.handle(null, param).toList()

        val expectedReverted = EnrichmentActivityConverter.convert(reverted)
            .let { EnrichmentActivityDtoConverter.convert(it, reverted = true) }

        val expectedLive = EnrichmentActivityConverter.convert(live)
            .let { EnrichmentActivityDtoConverter.convert(it, reverted = false) }

        coVerify(exactly = 1) { enrichmentActivityService.update(reverted) }
        coVerify(exactly = 1) { enrichmentActivityService.update(live) }
        coVerify(exactly = 1) { outgoingActivityEventListener.onEvents(listOf(expectedReverted, expectedLive)) }
        coVerify { esActivityRepository wasNot Called }
    }

    @Test
    fun `activities synced - reverted`() = runBlocking<Unit> {
        val reverted1 = randomUnionActivityBurn().copy(reverted = true)
        val reverted2 = randomUnionActivityTransfer().copy(reverted = true)

        val param = """{
            "blockchain": "ETHEREUM",
            "scope": "DB", 
            "sort": "DB_UPDATE_DESC", 
            "type": "ORDER",
            "reverted" : true
        }"""

        coEvery {
            activityService.getAllRevertedActivitiesSync(null, any(), SyncSortDto.DB_UPDATE_DESC, SyncTypeDto.ORDER)
        } returns Slice(null, listOf(reverted1, reverted2))

        job.handle(null, param).toList()

        coVerify(exactly = 1) { enrichmentActivityService.update(reverted1) }
        coVerify(exactly = 1) { enrichmentActivityService.update(reverted2) }
        coVerify { esActivityRepository wasNot Called }
        coVerify { outgoingActivityEventListener wasNot Called }
    }

    @Test
    fun `activities synced - bounded by date, desc`() = runBlocking<Unit> {
        val now = nowMillis()
        val activity1 = randomUnionActivityBurn().copy(lastUpdatedAt = now)
        val activity2 = randomUnionActivityBurn().copy(lastUpdatedAt = now.minusSeconds(10))
        // Should be skipped
        val activity3 = randomUnionActivityBurn().copy(lastUpdatedAt = now.minusSeconds(20))

        val param = """{
            "blockchain": "ETHEREUM",
            "scope": "DB",
            "sort": "DB_UPDATE_DESC",
            "type":"NFT",
            "batchSize": 1,
            "to" : "${now.minusSeconds(5)}"
        }"""

        coEvery {
            activityService.getAllActivitiesSync(null, any(), SyncSortDto.DB_UPDATE_DESC, SyncTypeDto.NFT)
        } returns Slice("1", listOf(activity1))

        coEvery {
            activityService.getAllActivitiesSync("1", any(), SyncSortDto.DB_UPDATE_DESC, SyncTypeDto.NFT)
        } returns Slice("2", listOf(activity2))

        job.handle(null, param).toList()

        coVerify(exactly = 1) { enrichmentActivityService.update(activity1) }
        coVerify(exactly = 1) { enrichmentActivityService.update(activity2) }
        coVerify(exactly = 0) { enrichmentActivityService.update(activity3) }
    }

    @Test
    fun `activities synced - bounded by date, asc`() = runBlocking<Unit> {
        val now = nowMillis()
        // Should be skipped
        val activity1 = randomUnionActivityBurn().copy(lastUpdatedAt = now)
        val activity2 = randomUnionActivityBurn().copy(lastUpdatedAt = now.minusSeconds(10))
        val activity3 = randomUnionActivityBurn().copy(lastUpdatedAt = now.minusSeconds(20))

        val param = """{
            "blockchain": "ETHEREUM",
            "scope": "DB",
            "sort": "DB_UPDATE_ASC",
            "type":"NFT",
            "batchSize": 1,
            "to" : "${now.minusSeconds(15)}"
        }"""

        coEvery {
            activityService.getAllActivitiesSync(null, any(), SyncSortDto.DB_UPDATE_ASC, SyncTypeDto.NFT)
        } returns Slice("1", listOf(activity3))

        coEvery {
            activityService.getAllActivitiesSync("1", any(), SyncSortDto.DB_UPDATE_ASC, SyncTypeDto.NFT)
        } returns Slice("2", listOf(activity2))

        job.handle(null, param).toList()

        coVerify(exactly = 0) { enrichmentActivityService.update(activity1) }
        coVerify(exactly = 1) { enrichmentActivityService.update(activity2) }
        coVerify(exactly = 1) { enrichmentActivityService.update(activity3) }
    }
}