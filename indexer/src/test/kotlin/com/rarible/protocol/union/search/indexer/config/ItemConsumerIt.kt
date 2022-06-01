package com.rarible.protocol.union.search.indexer.config

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.EsItem
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.ItemUpdateEventDto
import com.rarible.protocol.union.enrichment.repository.search.EsItemRepository
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigInteger
import java.time.Instant

@IntegrationTest
class ItemConsumerIt {

    @Autowired
    private lateinit var producer: RaribleKafkaProducer<ItemEventDto>

    @Autowired
    private lateinit var repository: EsItemRepository

    private val blockchain = BlockchainDto.ETHEREUM

    private val itemId = ItemIdDto(blockchain, "${randomAddress()}")
    private val item = ItemDto(
        id = itemId,
        blockchain = BlockchainDto.ETHEREUM,
        lazySupply = BigInteger.ONE,
        pending = emptyList(),
        mintedAt = Instant.now(),
        lastUpdatedAt = Instant.now(),
        supply = BigInteger.ONE,
        deleted = false,
        auctions = emptyList(),
        sellers = 1
    )

    @Test
    @Ignore("enable after merging ALPHA-424")
    internal fun `should save and find by id and feeRecipient`() {
        runBlocking {

            val event = ItemUpdateEventDto(
                eventId = itemId.fullId(),
                itemId = itemId,
                item = item
            )

            producer.send(KafkaEventFactory.itemEvent(event)).ensureSuccess()

            Wait.waitAssert {
                val actualItem = repository.findById(itemId.fullId())
                assert(actualItem)
            }
        }
    }

    private suspend fun assert(actualItem: EsItem?) {
        assertThat(actualItem).isNotNull
        actualItem!!
        assertThat(actualItem.itemId).isEqualTo(item.id.fullId())
        assertThat(actualItem.blockchain).isEqualTo(item.blockchain)
        assertThat(actualItem.mintedAt).isEqualTo(item.mintedAt)
        assertThat(actualItem.lastUpdatedAt).isEqualTo(item.lastUpdatedAt)
        assertThat(actualItem.collection).isEqualTo(item.collection?.fullId())
    }
}
