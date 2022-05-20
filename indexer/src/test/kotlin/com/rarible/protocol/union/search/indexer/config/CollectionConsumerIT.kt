package com.rarible.protocol.union.search.indexer.config

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomString
import com.rarible.core.test.wait.Wait
import com.rarible.protocol.union.core.event.KafkaEventFactory
import com.rarible.protocol.union.core.model.EsCollection
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.CollectionIdDto
import com.rarible.protocol.union.dto.CollectionMetaDto
import com.rarible.protocol.union.dto.CollectionUpdateEventDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.group
import com.rarible.protocol.union.enrichment.repository.search.EsCollectionRepository
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
class CollectionConsumerIT {

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

            Wait.waitAssert {
                val actualCollection = repository.findById(collectionId.fullId())!!
                assert(actualCollection)
            }
        }
    }

    private suspend fun assert(actualCollection: EsCollection) {
        assertThat(collection).isNotNull
        assertThat(actualCollection.collectionId).isEqualTo(collectionId.fullId())
        assertThat(actualCollection.name).isEqualTo(collection.name)
        assertThat(actualCollection.symbol).isEqualTo(collection.symbol)
        assertThat(actualCollection.owner).isEqualTo(collection.owner?.fullId())
        assertThat(actualCollection.meta).isNotNull
        assertThat(actualCollection.meta!!.name).isEqualTo(collection.meta!!.name)
        assertThat(actualCollection.meta!!.description).isEqualTo(collection.meta!!.description)
    }
}
