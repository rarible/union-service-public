package com.rarible.protocol.union.listener.service

import com.rarible.protocol.union.enrichment.converter.EnrichedItemConverter
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaPipeline
import com.rarible.protocol.union.enrichment.test.data.randomUnionMeta
import com.rarible.protocol.union.integration.ethereum.converter.EthItemConverter
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemTransferDto
import com.rarible.protocol.union.integration.ethereum.data.randomEthNftItemDto
import com.rarible.protocol.union.listener.test.AbstractIntegrationTest
import com.rarible.protocol.union.listener.test.IntegrationTest
import io.mockk.coEvery
import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigInteger
import java.util.function.Consumer

@IntegrationTest
class EnrichmentItemMetaLoadingIt : AbstractIntegrationTest() {

    @Autowired
    private lateinit var itemEventService: EnrichmentItemEventService

    @Test
    @Disabled("Works locally, fix under PT-953")
    fun `item update - meta not available - event without meta - event with meta`() = runWithKafka {
        val itemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(itemId).copy(
            lazySupply = BigInteger.ZERO,
            pending = emptyList()
        )
        val meta = randomUnionMeta()
        val unionItem = EthItemConverter.convert(ethItem, itemId.blockchain)
        coEvery { testItemMetaLoader.load(itemId) } coAnswers {
            delay(100L)
            meta
        }
        ethereumItemControllerApiMock.mockGetNftItemById(itemId, ethItem)
        itemEventService.onItemUpdated(unionItem)
        waitAssert {
            val events = findItemUpdates(itemId.value)
            assertThat(events).hasSize(2)
            assertThat(events[1].value.item)
                .isEqualTo(EnrichedItemConverter.convert(unionItem, meta = meta))
        }
    }

    @Test
    @Disabled("Works locally, fix under PT-953")
    fun `lazy item load meta synchronously`() = runWithKafka {
        val itemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(itemId).copy(
            lazySupply = BigInteger.ONE,
            pending = emptyList()
        )
        val meta = randomUnionMeta()
        val unionItem = EthItemConverter.convert(ethItem, itemId.blockchain)

        coEvery { testItemMetaLoader.load(itemId) } coAnswers {
            delay(100L)
            meta
        }
        ethereumItemControllerApiMock.mockGetNftItemById(itemId, ethItem)
        itemEventService.onItemUpdated(unionItem)
        waitAssert {
            val events = findItemUpdates(itemId.value)
            assertThat(events).hasSize(1)
            assertThat(events[0].value.item)
                .isEqualTo(EnrichedItemConverter.convert(unionItem, meta = meta))
        }
    }

    @Test
    @Disabled("Works locally, fix under PT-953")
    fun `just minted pending item load meta synchronously`() = runWithKafka {
        val itemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(itemId).copy(
            lazySupply = BigInteger.ZERO,
            supply = BigInteger.ZERO,
            pending = listOf(randomEthItemTransferDto())
        )
        val meta = randomUnionMeta()
        val unionItem = EthItemConverter.convert(ethItem, itemId.blockchain)
        coEvery { testItemMetaLoader.load(itemId) } coAnswers {
            delay(100L)
            meta
        }
        ethereumItemControllerApiMock.mockGetNftItemById(itemId, ethItem)
        itemEventService.onItemUpdated(unionItem)
        waitAssert {
            val events = findItemUpdates(itemId.value)
            assertThat(events).hasSize(1)
            assertThat(events[0].value.item)
                .isEqualTo(EnrichedItemConverter.convert(unionItem, meta = meta))
        }
    }

    @Test
    @Disabled("Works locally, fix under PT-953")
    fun `item update - meta not available - error loading - send 1 event without meta - then refresh`() = runWithKafka {
        val itemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(itemId)
        val meta = randomUnionMeta()
        val unionItem = EthItemConverter.convert(ethItem, itemId.blockchain)

        coEvery { testItemMetaLoader.load(itemId) } coAnswers {
            delay(100L)
            throw RuntimeException("error")
        }
        ethereumItemControllerApiMock.mockGetNftItemById(itemId, ethItem)
        itemEventService.onItemUpdated(unionItem)
        waitAssert {
            val events = findItemUpdates(itemId.value)
            assertThat(events).hasSize(1)
            assertThat(events[0].value.item)
                .isEqualTo(EnrichedItemConverter.convert(unionItem, meta = null))
        }
        // Schedule a successful update.
        coEvery { testItemMetaLoader.load(itemId) } coAnswers {
            delay(100L)
            meta
        }
        itemMetaService.schedule(itemId, ItemMetaPipeline.EVENT, false)
        // On loading meta, send an event.
        waitAssert {
            val events = findItemUpdates(itemId.value)
            assertThat(events).hasSize(2)
            assertThat(events[1].value.item)
                .isEqualTo(EnrichedItemConverter.convert(unionItem, meta = meta))
        }
    }

    @Test
    @Disabled("Works locally, fix under PT-953")
    fun `item update - meta available - send event immediately`() = runWithKafka<Unit> {
        val itemId = randomEthItemId()
        val ethItem = randomEthNftItemDto(itemId)
        val meta = randomUnionMeta()
        val unionItem = EthItemConverter.convert(ethItem, itemId.blockchain)

        itemMetaService.save(itemId, meta)
        ethereumItemControllerApiMock.mockGetNftItemById(itemId, ethItem)
        itemEventService.onItemUpdated(unionItem)
        waitAssert {
            val events = findItemUpdates(itemId.value)
            assertThat(events).hasSize(1).allSatisfy(Consumer {
                assertThat(it.value.item)
                    .isEqualTo(EnrichedItemConverter.convert(unionItem, meta = meta))
            })
        }
    }
}
