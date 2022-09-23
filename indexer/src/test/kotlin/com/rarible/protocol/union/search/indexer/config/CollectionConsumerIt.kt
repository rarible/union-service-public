package com.rarible.protocol.union.search.indexer.config

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.EsCollection
import com.rarible.protocol.union.core.test.WaitAssert
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CollectionMetaDto
import com.rarible.protocol.union.dto.CollectionUpdateEventDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.group
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.enrichment.test.data.randomEsCollection
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class CollectionConsumerIt {

    @Autowired
    private lateinit var producer: RaribleKafkaProducer<CollectionEventDto>

    @Autowired
    private lateinit var repository: EsCollectionRepository

    private val blockchain = BlockchainDto.ETHEREUM

    private val collectionId = CollectionIdDto(blockchain, "${randomAddress()}")
    private val collection = CollectionDto(
        id = collectionId,
        blockchain = blockchain,
        type = CollectionDto.Type.ERC721,
        name = randomString(),
        symbol = randomString(),
        owner = UnionAddress(blockchain.group(), "${randomAddress()}"),
        meta = CollectionMetaDto(
            name = randomString(),
            description = randomString(),
            feeRecipient = UnionAddress(blockchain.group(), "${randomAddress()}")
        )
    )

    @Test
    internal fun `should save and find by id and feeRecipient`() {
        runBlocking {

            val event = CollectionUpdateEventDto(
                eventId = randomString(),
                collectionId = collectionId,
                collection = collection
            )

            producer.send(KafkaEventFactory.collectionEvent(event)).ensureSuccess()

            WaitAssert.wait {
                val actualCollection = repository.findById(collectionId.fullId())
                assert(actualCollection)
            }
        }
    }

    @Test
    fun `should remove collection when event with error status comes`() = runBlocking<Unit> {
        // given
        val saved = randomEsCollection().copy(collectionId = collectionId.fullId())
        repository.bulk(listOf(saved), emptyList())
        val event = CollectionUpdateEventDto(
            eventId = randomString(),
            collectionId = collectionId,
            collection = collection.copy(status = CollectionDto.Status.ERROR)
        )

        // when
        producer.send(KafkaEventFactory.collectionEvent(event)).ensureSuccess()

        // then
        WaitAssert.wait {
            val notFound = repository.findById(collectionId.fullId())
            assertThat(notFound).isNull()
        }
    }

    private suspend fun assert(actualCollection: EsCollection?) {
        assertThat(actualCollection).isNotNull
        actualCollection as EsCollection
        assertThat(actualCollection.collectionId).isEqualTo(collectionId.fullId())
        assertThat(actualCollection.name).isEqualTo(collection.name)
        assertThat(actualCollection.symbol).isEqualTo(collection.symbol)
        assertThat(actualCollection.owner).isEqualTo(collection.owner?.fullId())
        assertThat(actualCollection.meta).isNotNull
        assertThat(actualCollection.meta!!.name).isEqualTo(collection.meta!!.name)
    }
}
