package com.rarible.protocol.union.worker.job

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.protocol.union.core.model.UnionAsset
import com.rarible.protocol.union.core.model.UnionEthErc20AssetType
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.dto.SyncSortDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.repository.ActivityRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentActivityService
import com.rarible.protocol.union.enrichment.test.data.randomEnrichmentMintActivity
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivityMint
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivitySale
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class SyncActivityJobTest {
    @InjectMockKs
    private lateinit var syncActivityJob: SyncActivityJob

    @MockK
    private lateinit var activityServiceRouter: BlockchainRouter<ActivityService>

    @MockK
    private lateinit var enrichmentActivityService: EnrichmentActivityService

    @MockK
    private lateinit var activityRepository: ActivityRepository

    @MockK
    private lateinit var activityService: ActivityService

    @Test
    fun run() = runBlocking<Unit> {
        val blockchain = BlockchainDto.ETHEREUM
        every { activityServiceRouter.getService(blockchain) } returns activityService
        val activity1 = randomUnionActivityMint(randomEthItemId())
        val activity2 = randomUnionActivityMint(randomEthItemId())
        val invalidActivity = randomUnionActivitySale(randomEthItemId())
            .copy(
                nft = UnionAsset(
                    type = UnionEthErc20AssetType(
                        contract = ContractAddress(
                            blockchain = BlockchainDto.ETHEREUM,
                            value = randomAddress().toString()
                        )
                    ), value = randomBigDecimal()
                ),
                payment = UnionAsset(
                    type = UnionEthErc20AssetType(
                        contract = ContractAddress(
                            blockchain = BlockchainDto.ETHEREUM,
                            value = randomAddress().toString()
                        )
                    ), value = randomBigDecimal()
                ),
            )
        coEvery {
            activityService.getAllActivitiesSync(
                continuation = null,
                size = 50,
                sort = SyncSortDto.DB_UPDATE_DESC,
                type = null
            )
        } returns Slice(continuation = "continuation1", entities = listOf(invalidActivity, activity1))

        coEvery {
            activityService.getAllActivitiesSync(
                continuation = "continuation1",
                size = 50,
                sort = SyncSortDto.DB_UPDATE_DESC,
                type = null
            )
        } returns Slice(continuation = null, entities = listOf(activity2))

        val enrichmentActivity1 = randomEnrichmentMintActivity()
        val enrichmentActivity2 = randomEnrichmentMintActivity()
        coEvery { enrichmentActivityService.enrich(activity1) } returns enrichmentActivity1
        coEvery { enrichmentActivityService.enrich(activity2) } returns enrichmentActivity2
        coEvery { activityRepository.save(any()) } returnsArgument 0

        assertThat(syncActivityJob.handle(null, blockchain.name).toList()).containsExactly("continuation1")

        coVerify {
            activityRepository.save(enrichmentActivity1)
            activityRepository.save(enrichmentActivity2)
        }
    }
}