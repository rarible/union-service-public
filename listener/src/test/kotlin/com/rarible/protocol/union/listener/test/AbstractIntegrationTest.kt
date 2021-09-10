package com.rarible.protocol.union.listener.test

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.*
import com.rarible.protocol.dto.FlowActivityDto
import com.rarible.protocol.dto.FlowOrderEventDto
import com.rarible.protocol.dto.FlowOwnershipEventDto
import com.rarible.protocol.union.dto.*
import com.rarible.protocol.union.dto.FlowOrderUpdateEventDto
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
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
    lateinit var ethOrderProducer: RaribleKafkaProducer<OrderEventDto>

    @Autowired
    lateinit var ethActivityProducer: RaribleKafkaProducer<ActivityDto>

    @Autowired
    lateinit var flowItemProducer: RaribleKafkaProducer<FlowNftItemEventDto>

    @Autowired
    lateinit var flowOwnershipProducer: RaribleKafkaProducer<FlowOwnershipEventDto>

    @Autowired
    lateinit var flowOrderProducer: RaribleKafkaProducer<FlowOrderEventDto>

    @Autowired
    lateinit var flowActivityProducer: RaribleKafkaProducer<FlowActivityDto>

    @Autowired
    lateinit var itemConsumer: RaribleKafkaConsumer<UnionItemEventDto>
    var itemEvents: Queue<KafkaMessage<UnionItemEventDto>>? = null
    private var itemJob: Deferred<Unit>? = null

    @Autowired
    lateinit var ownershipConsumer: RaribleKafkaConsumer<UnionOwnershipEventDto>
    var ownershipEvents: Queue<KafkaMessage<UnionOwnershipEventDto>>? = null
    private var ownershipJob: Deferred<Unit>? = null

    @Autowired
    lateinit var orderConsumer: RaribleKafkaConsumer<UnionOrderEventDto>
    var orderEvents: Queue<KafkaMessage<UnionOrderEventDto>>? = null
    private var orderJob: Deferred<Unit>? = null

    @Autowired
    lateinit var activityConsumer: RaribleKafkaConsumer<UnionActivityDto>
    var activityEvents: Queue<KafkaMessage<UnionActivityDto>>? = null
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

    fun findEthItemUpdates(itemId: String): List<KafkaMessage<EthItemUpdateEventDto>> {
        return filterByValueType(itemEvents as Queue<KafkaMessage<Any>>, EthItemUpdateEventDto::class.java)
            .filter { it.value.itemId.value == itemId }
    }

    fun findEthItemDeletions(itemId: String): List<KafkaMessage<EthItemDeleteEventDto>> {
        return filterByValueType(itemEvents as Queue<KafkaMessage<Any>>, EthItemDeleteEventDto::class.java)
            .filter { it.value.itemId.value == itemId }
    }

    fun findEthOwnershipUpdates(ownershipId: String): List<KafkaMessage<EthOwnershipUpdateEventDto>> {
        return filterByValueType(ownershipEvents as Queue<KafkaMessage<Any>>, EthOwnershipUpdateEventDto::class.java)
            .filter { it.value.ownershipId.value == ownershipId }
    }

    fun findEthOwnershipDeletions(ownershipId: String): List<KafkaMessage<EthOwnershipDeleteEventDto>> {
        return filterByValueType(ownershipEvents as Queue<KafkaMessage<Any>>, EthOwnershipDeleteEventDto::class.java)
            .filter { it.value.ownershipId.value == ownershipId }
    }

    fun findEthOrderUpdates(orderId: String): List<KafkaMessage<EthOrderUpdateEventDto>> {
        return filterByValueType(orderEvents as Queue<KafkaMessage<Any>>, EthOrderUpdateEventDto::class.java)
            .filter { it.value.orderId == orderId }
    }

    fun findFlowItemUpdates(itemId: String): List<KafkaMessage<FlowItemUpdateEventDto>> {
        return filterByValueType(itemEvents as Queue<KafkaMessage<Any>>, FlowItemUpdateEventDto::class.java)
            .filter { it.value.itemId.value == itemId }
    }

    fun findFlowItemDeletions(itemId: String): List<KafkaMessage<FlowItemDeleteEventDto>> {
        return filterByValueType(itemEvents as Queue<KafkaMessage<Any>>, FlowItemDeleteEventDto::class.java)
            .filter { it.value.itemId.value == itemId }
    }

    fun findFlowOwnershipUpdates(ownershipId: String): List<KafkaMessage<FlowOwnershipUpdateEventDto>> {
        return filterByValueType(ownershipEvents as Queue<KafkaMessage<Any>>, FlowOwnershipUpdateEventDto::class.java)
            .filter { it.value.ownershipId.value == ownershipId }
    }

    fun findFlowOwnershipDeletions(ownershipId: String): List<KafkaMessage<FlowOwnershipDeleteEventDto>> {
        return filterByValueType(ownershipEvents as Queue<KafkaMessage<Any>>, FlowOwnershipDeleteEventDto::class.java)
            .filter { it.value.ownershipId.value == ownershipId }
    }

    fun findFlowOrderUpdates(orderId: String): List<KafkaMessage<FlowOrderUpdateEventDto>> {
        return filterByValueType(orderEvents as Queue<KafkaMessage<Any>>, FlowOrderUpdateEventDto::class.java)
            .filter { it.value.orderId == orderId }
    }

    fun <T : EthActivityDto> findEthActivityUpdates(
        id: String,
        type: Class<T>
    ): List<KafkaMessage<T>> {
        return filterByValueType(activityEvents as Queue<KafkaMessage<Any>>, type)
            .filter { it.value.id.value == id }
    }

    fun <T : com.rarible.protocol.union.dto.FlowActivityDto> findFlowActivityUpdates(
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