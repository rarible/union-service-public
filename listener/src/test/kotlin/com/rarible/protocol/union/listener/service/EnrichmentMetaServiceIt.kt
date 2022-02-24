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

}
