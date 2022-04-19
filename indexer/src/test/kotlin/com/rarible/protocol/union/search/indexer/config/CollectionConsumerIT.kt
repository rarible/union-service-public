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
import com.rarible.protocol.union.search.indexer.test.IntegrationTest
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.elasticsearch.index.query.QueryBuilders.matchQuery
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.elasticsearch.core.ReactiveElasticsearchOperations
import org.springframework.data.elasticsearch.core.query.NativeSearchQuery
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder

@IntegrationTest
class CollectionConsumerIT {

    @Autowired
    private lateinit var producer: RaribleKafkaProducer<CollectionEventDto>

    @Autowired
    private lateinit var esOperations: ReactiveElasticsearchOperations

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
                val query = NativeSearchQueryBuilder().withQuery(
                    matchQuery("collectionId", collection.id.fullId())
                ).build()
                assertQuery(query)
            }

            Wait.waitAssert {
                val query = NativeSearchQueryBuilder().withQuery(
                    matchQuery("meta.feeRecipient", collection.meta!!.feeRecipient!!.fullId())
                ).build()
                assertQuery(query)
            }
        }
    }

    private suspend fun assertQuery(query: NativeSearchQuery) {
        val found = esOperations.search(query, EsCollection::class.java).awaitFirstOrNull()
        assertThat(found).isNotNull
        assertThat(found!!.id).isEqualTo(collectionId.fullId())
        assertThat(found.content.type).isEqualTo(collection.type.name)
        assertThat(found.content.name).isEqualTo(collection.name)
        assertThat(found.content.symbol).isEqualTo(collection.symbol)
        assertThat(found.content.owner).isEqualTo(collection.owner?.fullId())
        assertThat(found.content.meta).isNotNull
        assertThat(found.content.meta!!.name).isEqualTo(collection.meta!!.name)
        assertThat(found.content.meta!!.description).isEqualTo(collection.meta!!.description)
        assertThat(found.content.meta!!.feeRecipient).isEqualTo(collection.meta!!.feeRecipient?.fullId())
    }
}
