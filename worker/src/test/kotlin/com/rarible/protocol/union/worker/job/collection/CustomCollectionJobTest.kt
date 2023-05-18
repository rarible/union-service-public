package com.rarible.protocol.union.worker.job.collection

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.model.UnionInternalBlockchainEvent
import com.rarible.protocol.union.core.model.UnionInternalItemEvent
import com.rarible.protocol.union.core.model.UnionItemChangeEvent
import com.rarible.protocol.union.core.producer.UnionInternalBlockchainEventProducer
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class CustomCollectionJobTest {

    private val blockchainEventProducer: RaribleKafkaProducer<UnionInternalBlockchainEvent> = mockk {
        coEvery { send(any<KafkaMessage<UnionInternalBlockchainEvent>>()) } returns mockk()
    }

    private val eventProducer = UnionInternalBlockchainEventProducer(
        mapOf(BlockchainDto.ETHEREUM to blockchainEventProducer)
    )

    private val itemFetcherProvider: CustomCollectionItemFetcherFactory = mockk()
    private val updater: CustomCollectionUpdater = mockk {
        coEvery { update(any()) } returns Unit
    }

    private val fetcher1: CustomCollectionItemFetcher = mockk()
    private val fetcher2: CustomCollectionItemFetcher = mockk()
    private val fetcher3: CustomCollectionItemFetcher = mockk()

    private val batchSize = 50

    private val job = CustomCollectionJob(
        eventProducer,
        itemFetcherProvider,
        listOf(updater)
    )

    @BeforeEach
    fun beforeEach() {
        clearMocks(fetcher1, fetcher2, fetcher3)
    }

    @Test
    fun `execute - ok`() = runBlocking<Unit> {
        val name = "test"

        val item1 = randomUnionItem(randomEthItemId())
        val item2 = randomUnionItem(randomEthItemId())
        val item3 = randomUnionItem(randomEthItemId())

        val state1 = randomString()
        val state2 = randomString()
        val state3 = randomString()

        coEvery { itemFetcherProvider.get(name) } returns listOf(fetcher1, fetcher2, fetcher3)

        coEvery { fetcher1.next(null, batchSize) } returns CustomCollectionItemBatch(state1, listOf(item1))
        coEvery { fetcher1.next(state1, batchSize) } returns CustomCollectionItemBatch.empty()

        coEvery { fetcher2.next(null, batchSize) } returns CustomCollectionItemBatch.empty()

        coEvery { fetcher3.next(null, batchSize) } returns CustomCollectionItemBatch(state2, listOf(item2))
        coEvery { fetcher3.next(state2, batchSize) } returns CustomCollectionItemBatch(state3, listOf(item3))
        coEvery { fetcher3.next(state3, batchSize) } returns CustomCollectionItemBatch.empty()

        val continuation1 = job.migrate(name, null)
        assertThat(continuation1).isEqualTo("0_$state1")

        val continuation2 = job.migrate(name, continuation1)
        assertThat(continuation2).isEqualTo("2_$state2")

        val continuation3 = job.migrate(name, continuation2)
        assertThat(continuation3).isEqualTo("2_$state3")

        val continuation4 = job.migrate(name, continuation3)
        assertThat(continuation4).isNull()

        verifyItemChangeEvent(item1.id)
        verifyItemChangeEvent(item2.id)
        verifyItemChangeEvent(item3.id)

        coVerify { updater.update(item1) }
        coVerify { updater.update(item2) }
        coVerify { updater.update(item3) }
    }

    private fun verifyItemChangeEvent(itemId: ItemIdDto) {
        coVerify {
            blockchainEventProducer.send(match<KafkaMessage<UnionInternalBlockchainEvent>> {
                val event = it.value
                val changeEvent = (event as UnionInternalItemEvent).event as UnionItemChangeEvent
                changeEvent.itemId == itemId
            })
        }
    }

}