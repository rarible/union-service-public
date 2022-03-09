package com.rarible.protocol.union.enrichment.service

import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OwnershipSourceDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityMint
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityTransfer
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthOwnershipId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class EnrichmentActivityServiceTest {

    private val typesMint = listOf(ActivityTypeDto.MINT)
    private val typesTransfer = listOf(ActivityTypeDto.TRANSFER)

    private val blockchain = BlockchainDto.ETHEREUM
    private val activityService: ActivityService = mockk()

    private lateinit var service: EnrichmentActivityService

    @BeforeEach
    fun beforeEach() {
        clearMocks(activityService)
        every { activityService.blockchain } returns blockchain
        service = EnrichmentActivityService(
            BlockchainRouter(listOf(activityService), listOf(blockchain))
        )
    }

    @Test
    fun `mint source`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val activity = randomUnionActivityMint(itemId)

        // Mint found, owner matches
        coEvery {
            activityService.getActivitiesByItem(typesMint, itemId.value, null, 1, ActivitySortDto.LATEST_FIRST)
        } returns Slice(null, listOf(activity))

        val source = service.getOwnershipSource(itemId.toOwnership(activity.owner.value))

        assertThat(source).isEqualTo(OwnershipSourceDto.MINT)
    }

    @Test
    fun `purchase source`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val activity = randomUnionActivityTransfer(itemId).copy(purchase = true)
        val otherOwnerTransfer = randomUnionActivityTransfer(itemId)
        val otherOwnerMint = randomUnionActivityMint(itemId)

        // Mint found, but owner is different
        coEvery {
            activityService.getActivitiesByItem(typesMint, any(), any(), any(), any())
        } returns Slice(null, listOf(otherOwnerMint))

        // Transfer found for target owner
        coEvery {
            activityService.getActivitiesByItem(typesTransfer, itemId.value, null, 100, ActivitySortDto.LATEST_FIRST)
        } returns Slice(null, listOf(otherOwnerTransfer, activity))

        val source = service.getOwnershipSource(itemId.toOwnership(activity.owner.value))

        assertThat(source).isEqualTo(OwnershipSourceDto.PURCHASE)
    }

    @Test
    fun `transfer source - found`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val activity = randomUnionActivityTransfer(itemId).copy(purchase = false)

        // Mint not found
        coEvery {
            activityService.getActivitiesByItem(listOf(ActivityTypeDto.MINT), any(), any(), any(), any())
        } returns Slice.empty()

        // Transfer found, purchase = false
        coEvery {
            activityService.getActivitiesByItem(typesTransfer, itemId.value, null, 100, ActivitySortDto.LATEST_FIRST)
        } returns Slice(null, listOf(activity))

        val source = service.getOwnershipSource(itemId.toOwnership(activity.owner.value))

        assertThat(source).isEqualTo(OwnershipSourceDto.TRANSFER)
    }

    @Test
    fun `transfer type - nothing found`() = runBlocking<Unit> {
        val itemId = randomEthItemId()

        // Mint not found
        coEvery {
            activityService.getActivitiesByItem(listOf(ActivityTypeDto.MINT), any(), any(), any(), any())
        } returns Slice.empty()

        // Transfer not found
        coEvery {
            activityService.getActivitiesByItem(typesTransfer, itemId.value, null, 100, ActivitySortDto.LATEST_FIRST)
        } returns Slice.empty()

        val source = service.getOwnershipSource(randomEthOwnershipId(itemId))

        assertThat(source).isEqualTo(OwnershipSourceDto.TRANSFER)
    }

}