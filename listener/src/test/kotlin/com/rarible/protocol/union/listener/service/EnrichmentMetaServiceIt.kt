package com.rarible.protocol.union.listener.service

import com.rarible.core.test.wait.Wait
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
    fun `get available or schedule - null - then update - then get`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val meta = randomUnionItem(itemId).meta!!
        coEvery { testUnionMetaLoader.load(itemId) } returns meta
        assertThat(enrichmentMetaService.getAvailableMetaOrScheduleLoading(itemId)).isNull()
        delay(500)
        assertThat(enrichmentMetaService.getAvailableMetaOrScheduleLoading(itemId)).isEqualTo(meta)
    }

    @Test
    fun `get available and wait - null on timeout - then get`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val meta = randomUnionItem(itemId).meta!!
        coEvery { testUnionMetaLoader.load(itemId) } coAnswers {
            delay(1000)
            meta
        }
        assertThat(enrichmentMetaService.getAvailableMetaOrScheduleLoadingAndWaitWithTimeout(itemId, Duration.ofMillis(500))).isNull()
        Wait.waitAssert(timeout = Duration.ofSeconds(2)) {
            assertThat(enrichmentMetaService.getAvailableMetaOrScheduleLoading(itemId)).isEqualTo(meta)
        }
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
            enrichmentMetaService.getAvailableMetaOrScheduleLoadingAndWaitWithTimeout(
                itemId = itemId,
                loadingWaitTimeout = Duration.ofMillis(5000)
            )
        ).isEqualTo(meta)
    }

}
