package com.rarible.protocol.union.enrichment.custom.collection

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.union.core.model.UnionInternalBlockchainEvent
import com.rarible.protocol.union.core.model.UnionInternalItemEvent
import com.rarible.protocol.union.core.model.UnionItemChangeEvent
import com.rarible.protocol.union.core.producer.UnionInternalBlockchainEventProducer
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.enrichment.test.data.randomUnionItem
import com.rarible.protocol.union.integration.ethereum.data.randomEthItemId
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test

class CustomCollectionMigratorTest {

    private val blockchainEventProducer: RaribleKafkaProducer<UnionInternalBlockchainEvent> = mockk {
        coEvery { send(any<KafkaMessage<UnionInternalBlockchainEvent>>()) } returns mockk()
    }

    private val collectionUpdater: CustomCollectionUpdater = mockk {
        coEvery { update(any()) } returns Unit
    }

    private val migrator = CustomCollectionMigrator(
        UnionInternalBlockchainEventProducer(mapOf(BlockchainDto.ETHEREUM to blockchainEventProducer)),
        listOf(collectionUpdater)
    )

    @Test
    fun `migrate - ok`() = runBlocking<Unit> {
        val item1 = randomUnionItem(randomEthItemId())
        val item2 = randomUnionItem(randomEthItemId())
        val item3 = randomUnionItem(randomEthItemId())

        migrator.migrate(listOf(item1, item2, item3))

        verifyItemChangeEvent(item1.id)
        verifyItemChangeEvent(item2.id)
        verifyItemChangeEvent(item3.id)

        coVerify { collectionUpdater.update(item1) }
        coVerify { collectionUpdater.update(item2) }
        coVerify { collectionUpdater.update(item3) }
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