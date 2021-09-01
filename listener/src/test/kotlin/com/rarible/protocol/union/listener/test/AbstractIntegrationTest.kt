package com.rarible.protocol.union.listener.test

import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.NftItemEventDto
import com.rarible.protocol.dto.NftOwnershipEventDto
import com.rarible.protocol.dto.OrderEventDto
import com.rarible.protocol.union.dto.*
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

    fun <T> runWithKafka(block: suspend CoroutineScope.() -> T): T = runBlocking<T> {
        orderEvents = LinkedBlockingQueue()
        orderJob = async { orderConsumer.receive().collect { orderEvents?.add(it) } }

        ownershipEvents = LinkedBlockingQueue()
        ownershipJob = async { ownershipConsumer.receive().collect { ownershipEvents?.add(it) } }

        itemEvents = LinkedBlockingQueue()
        itemJob = async { itemConsumer.receive().collect { itemEvents?.add(it) } }

        val result = try {
            block()
        } finally {
            itemJob?.cancel()
            ownershipJob?.cancel()
            orderJob?.cancel()
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