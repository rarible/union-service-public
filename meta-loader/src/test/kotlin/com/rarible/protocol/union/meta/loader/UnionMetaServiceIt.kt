package com.rarible.protocol.union.meta.loader

import com.rarible.protocol.union.enrichment.service.ItemMetaService
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
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
    private lateinit var itemMetaService: ItemMetaService

    @Test
    fun `get available or schedule - null - then update - then get`() = runBlocking<Unit> {
        val itemId = randomEthItemId()
        val meta = randomUnionMeta()
        coEvery { testUnionMetaLoader.load(itemId) } returns meta
        assertThat(itemMetaService.get(itemId, false, "default")).isNull()  // TODO PT-49
        delay(500)
        assertThat(itemMetaService.get(itemId, false, "default")).isEqualTo(meta)  // TODO PT-49
    }
}
