package com.rarible.protocol.union.listener.service

import com.rarible.core.test.wait.Wait
import com.rarible.protocol.union.enrichment.converter.EnrichedItemConverter
import com.rarible.protocol.union.integration.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.coEvery
import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono

@IntegrationTest
class EnrichmentItemMetaLoadingIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var itemEventService: EnrichmentItemEventService

    @Test
    fun `item update - meta not available - event with null - then meta loaded - send 2nd update event`() = runWithKafka {
        val itemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(itemId)
        val (unionItem, meta) = EthItemConverter.convert(ethItem, itemId.blockchain).let {
            it.copy(meta = null) to it.meta!!
        }
        coEvery { testUnionMetaLoader.load(itemId) } coAnswers {
            delay(100L)
            meta
        }
        coEvery { testEthereumItemApi.getNftItemById(itemId.value) } returns ethItem.toMono()
        itemEventService.onItemUpdated(unionItem)
        Wait.waitAssert {
            val events = findItemUpdates(itemId.value)
            assertThat(events).hasSize(1)
            assertThat(events[0].value.item)
                .isEqualTo(EnrichedItemConverter.convert(unionItem, meta = null))
        }
        Wait.waitAssert {
            val events = findItemUpdates(itemId.value)
            assertThat(events).hasSize(2)
            assertThat(events[1].value.item)
                .isEqualTo(EnrichedItemConverter.convert(unionItem, meta = meta))
        }
    }

}
