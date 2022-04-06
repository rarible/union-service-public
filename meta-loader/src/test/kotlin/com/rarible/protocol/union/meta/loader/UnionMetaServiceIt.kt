package com.rarible.protocol.union.meta.loader

import com.rarible.core.content.meta.loader.ContentReceiver
import com.rarible.protocol.union.enrichment.configuration.UnionMetaProperties
import com.rarible.protocol.union.enrichment.meta.UnionMetaService
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.meta.loader.test.AbstractIntegrationTest
import com.rarible.protocol.union.meta.loader.test.IntegrationTest
import io.mockk.coEvery
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class UnionMetaServiceIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var unionMetaService: UnionMetaService

    @Test
    fun `get available or schedule - null - then update - then get`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val meta = randomUnionItem(itemId).meta!!
        coEvery { testUnionMetaLoader.load(itemId) } returns meta
        assertThat(unionMetaService.getAvailableMetaOrScheduleLoading(itemId)).isNull()
        delay(500)
        assertThat(unionMetaService.getAvailableMetaOrScheduleLoading(itemId)).isEqualTo(meta)
    }

}
