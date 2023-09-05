package com.rarible.protocol.union.listener.handler.internal

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigDecimal
import com.rarible.protocol.union.core.model.UnionAsset
import com.rarible.protocol.union.core.model.UnionEthErc20AssetType
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ContractAddress
import com.rarible.protocol.union.enrichment.service.EnrichmentActivityEventService
import com.rarible.protocol.union.enrichment.service.ReconciliationEventService
import com.rarible.protocol.union.enrichment.test.data.randomUnionActivitySale
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
internal class UnionInternalActivityEventHandlerTest {
    @MockK
    private lateinit var enrichmentActivityEventService: EnrichmentActivityEventService

    @MockK
    private lateinit var reconciliationEventService: ReconciliationEventService

    @InjectMockKs
    private lateinit var unionInternalActivityEventHandler: UnionInternalActivityEventHandler

    @Test
    fun `ignore not valid activities`() = runBlocking<Unit> {
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

        unionInternalActivityEventHandler.onEvent(invalidActivity)

        coVerify {
            enrichmentActivityEventService wasNot Called
            reconciliationEventService wasNot Called
        }
    }

    @Test
    fun `valid activity`() = runBlocking<Unit> {
        val activity = randomUnionActivitySale(randomEthItemId())
        coEvery { enrichmentActivityEventService.onActivity(activity) } returns Unit

        unionInternalActivityEventHandler.onEvent(activity)

        coVerify {
            enrichmentActivityEventService.onActivity(activity)
        }
    }
}
