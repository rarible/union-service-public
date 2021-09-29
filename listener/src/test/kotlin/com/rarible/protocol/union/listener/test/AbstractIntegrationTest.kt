package com.rarible.protocol.union.listener.test

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.dto.FlowNftItemEventDto
import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.dto.FlowOwnershipEventDto
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ItemDeleteEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.ItemUpdateEventDto
import com.rarible.protocol.union.dto.OrderEventDto
import com.rarible.protocol.union.dto.OrderUpdateEventDto
import com.rarible.protocol.union.dto.OwnershipDeleteEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.dto.OwnershipUpdateEventDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.springframework.beans.factory.annotation.Autowired
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

@FlowPreview
@Suppress("UNCHECKED_CAST")
abstract class AbstractIntegrationTest {

    @Autowired
    lateinit var ethItemProducer: RaribleKafkaProducer<NftItemEventDto>

    @Autowired
    lateinit var ethOwnershipProducer: RaribleKafkaProducer<NftOwnershipEventDto>

    @Autowired
    lateinit var ethOrderProducer: RaribleKafkaProducer<com.rarible.protocol.dto.OrderEventDto>

    @Autowired
    lateinit var ethActivityProducer: RaribleKafkaProducer<com.rarible.protocol.dto.ActivityDto>

    @Autowired
    lateinit var flowItemProducer: RaribleKafkaProducer<FlowNftItemEventDto>

    @Autowired
    lateinit var flowOwnershipProducer: RaribleKafkaProducer<FlowOwnershipEventDto>

    @Autowired
    lateinit var flowOrderProducer: RaribleKafkaProducer<FlowOrderEventDto>

    @Autowired
    lateinit var flowActivityProducer: RaribleKafkaProducer<FlowActivityDto>

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

    fun <T> runWithKafka(block: suspend CoroutineScope.() -> T): T = runBlocking<T> {
        orderEvents = LinkedBlockingQueue()
        orderJob = async { orderConsumer.receive().collect { orderEvents?.add(it) } }

        ownershipEvents = LinkedBlockingQueue()
        ownershipJob = async { ownershipConsumer.receive().collect { ownershipEvents?.add(it) } }

        itemEvents = LinkedBlockingQueue()
        itemJob = async { itemConsumer.receive().collect { itemEvents?.add(it) } }

        activityEvents = LinkedBlockingQueue()
        activityJob = async { activityConsumer.receive().collect { activityEvents?.add(it) } }

        val result = try {
            block()
        } finally {
            itemJob?.cancelAndJoin()
            ownershipJob?.cancelAndJoin()
            orderJob?.cancelAndJoin()
            activityJob?.cancelAndJoin()
        }
        result
    }

    fun findEthItemUpdates(itemId: String): List<KafkaMessage<ItemUpdateEventDto>> {
        return filterByValueType(itemEvents as Queue<KafkaMessage<Any>>, ItemUpdateEventDto::class.java)
            .filter { it.value.itemId.value == itemId }
    }

    fun findEthItemDeletions(itemId: String): List<KafkaMessage<ItemDeleteEventDto>> {
        return filterByValueType(itemEvents as Queue<KafkaMessage<Any>>, ItemDeleteEventDto::class.java)
            .filter { it.value.itemId.value == itemId }
    }

    fun findEthOwnershipUpdates(ownershipId: String): List<KafkaMessage<OwnershipUpdateEventDto>> {
        return filterByValueType(ownershipEvents as Queue<KafkaMessage<Any>>, OwnershipUpdateEventDto::class.java)
            .filter { it.value.ownershipId.value == ownershipId }
    }

    fun findEthOwnershipDeletions(ownershipId: String): List<KafkaMessage<OwnershipDeleteEventDto>> {
        return filterByValueType(ownershipEvents as Queue<KafkaMessage<Any>>, OwnershipDeleteEventDto::class.java)
            .filter { it.value.ownershipId.value == ownershipId }
    }

    fun findEthOrderUpdates(orderId: String): List<KafkaMessage<OrderUpdateEventDto>> {
        return filterByValueType(orderEvents as Queue<KafkaMessage<Any>>, OrderUpdateEventDto::class.java)
            .filter { it.value.orderId.value == orderId }
    }

    fun findFlowItemUpdates(itemId: String): List<KafkaMessage<ItemUpdateEventDto>> {
        return filterByValueType(itemEvents as Queue<KafkaMessage<Any>>, ItemUpdateEventDto::class.java)
            .filter { it.value.itemId.value == itemId }
    }

    fun findFlowItemDeletions(itemId: String): List<KafkaMessage<ItemDeleteEventDto>> {
        return filterByValueType(itemEvents as Queue<KafkaMessage<Any>>, ItemDeleteEventDto::class.java)
            .filter { it.value.itemId.value == itemId }
    }

    fun findFlowOwnershipUpdates(ownershipId: String): List<KafkaMessage<OwnershipUpdateEventDto>> {
        return filterByValueType(ownershipEvents as Queue<KafkaMessage<Any>>, OwnershipUpdateEventDto::class.java)
            .filter { it.value.ownershipId.value == ownershipId }
    }

    fun findFlowOwnershipDeletions(ownershipId: String): List<KafkaMessage<OwnershipDeleteEventDto>> {
        return filterByValueType(ownershipEvents as Queue<KafkaMessage<Any>>, OwnershipDeleteEventDto::class.java)
            .filter { it.value.ownershipId.value == ownershipId }
    }

    fun findFlowOrderUpdates(orderId: String): List<KafkaMessage<OrderUpdateEventDto>> {
        return filterByValueType(orderEvents as Queue<KafkaMessage<Any>>, OrderUpdateEventDto::class.java)
            .filter { it.value.orderId.value == orderId }
    }

    fun <T : ActivityDto> findEthActivityUpdates(
        id: String,
        type: Class<T>
    ): List<KafkaMessage<T>> {
        return filterByValueType(activityEvents as Queue<KafkaMessage<Any>>, type)
            .filter { it.value.id.value == id }
    }

    fun <T : ActivityDto> findFlowActivityUpdates(
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