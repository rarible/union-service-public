package com.rarible.protocol.union.listener.tezos

import com.rarible.core.content.meta.loader.ContentMetaReceiver
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.data.randomString
import com.rarible.dipdup.client.core.model.DipDupActivity
import com.rarible.dipdup.client.core.model.DipDupOrder
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.CollectionUpdateEventDto
import com.rarible.protocol.union.dto.ItemDeleteEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.ItemUpdateEventDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OrderUpdateEventDto
import com.rarible.protocol.union.dto.OwnershipDeleteEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.OwnershipUpdateEventDto
import com.rarible.protocol.union.enrichment.configuration.UnionMetaProperties
import com.rarible.protocol.union.enrichment.meta.UnionMetaLoader
import com.rarible.tzkt.client.OwnershipClient
import com.rarible.tzkt.client.TokenClient
import io.mockk.clearMocks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

@Suppress("UNCHECKED_CAST")
abstract class AbstractDipDupIntegrationTest {

    @Autowired
    @Qualifier("test.union.meta.loader")
    lateinit var testUnionMetaLoader: UnionMetaLoader

    @Autowired
    @Qualifier("test.content.meta.receiver")
    lateinit var testContentMetaReceiver: ContentMetaReceiver

    @Autowired
    lateinit var unionMetaProperties: UnionMetaProperties

    //--------------------- TEZOS ---------------------//

    @Autowired
    lateinit var testTezosOrderApi: com.rarible.protocol.tezos.api.client.OrderControllerApi

    @Autowired
    lateinit var testTezosItemApi: com.rarible.protocol.tezos.api.client.NftItemControllerApi

    @Autowired
    lateinit var testTezosOwnershipApi: com.rarible.protocol.tezos.api.client.NftOwnershipControllerApi

    @Autowired
    lateinit var ownershipClient: OwnershipClient

    @Autowired
    lateinit var tokenClient: TokenClient


    @Autowired
    lateinit var dipDupOrderProducer: RaribleKafkaProducer<DipDupOrder>

    @Autowired
    lateinit var dipDupActivityProducer: RaribleKafkaProducer<DipDupActivity>

    @Autowired
    lateinit var collectionConsumer: RaribleKafkaConsumer<CollectionEventDto>
    var collectionEvents: Queue<KafkaMessage<CollectionEventDto>>? = null
    private var collectionJob: Deferred<Unit>? = null

    @Autowired
    lateinit var itemConsumer: RaribleKafkaConsumer<ItemEventDto>
    var itemEvents: Queue<KafkaMessage<ItemEventDto>>? = null
    private var itemJob: Deferred<Unit>? = null

    @Autowired
    lateinit var ownershipConsumer: RaribleKafkaConsumer<OwnershipEventDto>
    var ownershipEvents: Queue<KafkaMessage<OwnershipEventDto>>? = null
    private var ownershipJob: Deferred<Unit>? = null

    @Autowired
    lateinit var orderConsumer: RaribleKafkaConsumer<OrderEventDto>
    var orderEvents: Queue<KafkaMessage<OrderEventDto>>? = null
    private var orderJob: Deferred<Unit>? = null

    @Autowired
    lateinit var activityConsumer: RaribleKafkaConsumer<ActivityDto>
    var activityEvents: Queue<KafkaMessage<ActivityDto>>? = null
    private var activityJob: Deferred<Unit>? = null

    @BeforeEach
    fun cleanupMetaMocks() {
        clearMocks(testUnionMetaLoader)
        clearMocks(ownershipClient)
        clearMocks(tokenClient)
    }

    fun <T> runWithKafka(block: suspend CoroutineScope.() -> T): T = runBlocking {
        orderEvents = LinkedBlockingQueue()
        orderJob = async { orderConsumer.receiveAutoAck().collect { orderEvents?.add(it) } }

        ownershipEvents = LinkedBlockingQueue()
        ownershipJob = async { ownershipConsumer.receiveAutoAck().collect { ownershipEvents?.add(it) } }

        itemEvents = LinkedBlockingQueue()
        itemJob = async { itemConsumer.receiveAutoAck().collect { itemEvents?.add(it) } }

        collectionEvents = LinkedBlockingQueue()
        collectionJob = async { collectionConsumer.receiveAutoAck().collect { collectionEvents?.add(it) } }

        activityEvents = LinkedBlockingQueue()
        activityJob = async { activityConsumer.receiveAutoAck().collect { activityEvents?.add(it) } }

        val result = try {
            block()
        } finally {
            itemJob?.cancelAndJoin()
            ownershipJob?.cancelAndJoin()
            orderJob?.cancelAndJoin()
            activityJob?.cancelAndJoin()
            collectionJob?.cancelAndJoin()
        }
        result
    }

    fun findItemUpdates(itemId: String): List<KafkaMessage<ItemUpdateEventDto>> {
        return filterByValueType(itemEvents as Queue<KafkaMessage<Any>>, ItemUpdateEventDto::class.java)
            .filter { it.value.itemId.value == itemId }
    }

    fun findCollectionUpdates(collectionId: String): List<KafkaMessage<CollectionUpdateEventDto>> {
        return filterByValueType(collectionEvents as Queue<KafkaMessage<Any>>, CollectionUpdateEventDto::class.java)
            .filter { it.value.collectionId.value == collectionId }
    }

    fun findItemDeletions(itemId: String): List<KafkaMessage<ItemDeleteEventDto>> {
        return filterByValueType(itemEvents as Queue<KafkaMessage<Any>>, ItemDeleteEventDto::class.java)
            .filter { it.value.itemId.value == itemId }
    }

    fun findOwnershipUpdates(ownershipId: String): List<KafkaMessage<OwnershipUpdateEventDto>> {
        return filterByValueType(ownershipEvents as Queue<KafkaMessage<Any>>, OwnershipUpdateEventDto::class.java)
            .filter { it.value.ownershipId.value == ownershipId }
    }

    fun findOwnershipDeletions(ownershipId: String): List<KafkaMessage<OwnershipDeleteEventDto>> {
        return filterByValueType(ownershipEvents as Queue<KafkaMessage<Any>>, OwnershipDeleteEventDto::class.java)
            .filter { it.value.ownershipId.value == ownershipId }
    }

    fun findOrderUpdates(orderId: String): List<KafkaMessage<OrderUpdateEventDto>> {
        return filterByValueType(orderEvents as Queue<KafkaMessage<Any>>, OrderUpdateEventDto::class.java)
            .filter { it.value.orderId.value == orderId }
    }

    fun <T : ActivityDto> findActivityUpdates(
        id: String,
        type: Class<T>
    ): List<KafkaMessage<T>> {
        return filterByValueType(activityEvents as Queue<KafkaMessage<Any>>, type)
            .filter { it.value.id.value == id }
    }

    private fun <T> filterByValueType(messages: Queue<KafkaMessage<Any>>, type: Class<T>): Collection<KafkaMessage<T>> {
        return messages.filter {
            type.isInstance(it.value)
        }.map {
            it as KafkaMessage<T>
        }
    }

    fun <T> message(dto: T): KafkaMessage<T> {
        return KafkaMessage(
            id = randomString(),
            key = randomString(),
            value = dto
        )
    }
}
