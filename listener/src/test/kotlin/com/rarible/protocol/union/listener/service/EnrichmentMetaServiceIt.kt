package com.rarible.protocol.union.listener.service

import com.rarible.protocol.union.enrichment.service.EnrichmentMetaService
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.coEvery
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration

@IntegrationTest
class EnrichmentMetaServiceIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var enrichmentMetaService: EnrichmentMetaService

    @Test
    fun `get available - null - then update - then get`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val meta = randomUnionItem(itemId).meta!!
        coEvery { testUnionMetaLoader.load(itemId) } returns meta
        assertThat(enrichmentMetaService.getAvailableMeta(itemId)).isNull()
        enrichmentMetaService.scheduleLoading(itemId)
        delay(500)
        assertThat(enrichmentMetaService.getAvailableMeta(itemId)).isEqualTo(meta)
    }

    @Test
    fun `get available - null on timeout - then get`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val meta = randomUnionItem(itemId).meta!!
        coEvery { testUnionMetaLoader.load(itemId) } coAnswers {
            delay(1000)
            meta
        }
        assertThat(enrichmentMetaService.getAvailableMetaOrScheduleLoading(itemId)).isNull()
        delay(1000)
        assertThat(enrichmentMetaService.getAvailableMeta(itemId)).isEqualTo(meta)
    }

    @Test
    fun `get available - wait for loading with timeout`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val meta = randomUnionItem(itemId).meta!!
        coEvery { testUnionMetaLoader.load(itemId) } coAnswers {
            delay(1000)
            meta
        }
        assertThat(
            enrichmentMetaService.getAvailableMetaOrScheduleAndWait(
                itemId = itemId,
                loadingWaitTimeout = Duration.ofMillis(5000)
            )
        ).isEqualTo(meta)
    }

}
