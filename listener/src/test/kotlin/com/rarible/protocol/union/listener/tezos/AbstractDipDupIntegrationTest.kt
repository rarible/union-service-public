package com.rarible.protocol.union.listener.tezos

import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.dipdup.client.core.model.DipDupActivity
import com.rarible.dipdup.client.core.model.DipDupOrder
import com.rarible.dipdup.listener.model.DipDupCollectionEvent
import com.rarible.protocol.union.core.test.TestUnionEventHandler
import com.rarible.protocol.union.core.test.WaitAssert
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.CollectionUpdateEventDto
import com.rarible.protocol.union.dto.ItemDeleteEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.ItemUpdateEventDto
import com.rarible.protocol.union.dto.OrderUpdateEventDto
import com.rarible.protocol.union.dto.OwnershipDeleteEventDto
import com.rarible.protocol.union.dto.OwnershipUpdateEventDto
import com.rarible.tzkt.client.OwnershipClient
import com.rarible.tzkt.client.TokenClient
import io.mockk.clearMocks
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired

@Suppress("UNCHECKED_CAST")
abstract class AbstractDipDupIntegrationTest {

    // --------------------- TEZOS ---------------------//

    @Autowired
    lateinit var ownershipClient: OwnershipClient

    @Autowired
    lateinit var tokenClient: TokenClient

    @Autowired
    lateinit var dipDupOrderProducer: RaribleKafkaProducer<DipDupOrder>

    @Autowired
    lateinit var dipDupActivityProducer: RaribleKafkaProducer<DipDupActivity>

    @Autowired
    lateinit var dipDupCollectionProducer: RaribleKafkaProducer<DipDupCollectionEvent>

    @Autowired
    lateinit var testEventHandlers: List<TestUnionEventHandler<*>>

    @Autowired
    lateinit var testCollectionEventHandler: TestUnionEventHandler<ItemEventDto>

    @Autowired
    lateinit var testItemEventHandler: TestUnionEventHandler<ItemEventDto>

    @Autowired
    lateinit var testOwnershipEventHandler: TestUnionEventHandler<ItemEventDto>

    @Autowired
    lateinit var testOrderEventHandler: TestUnionEventHandler<ItemEventDto>

    @Autowired
    lateinit var testActivityEventHandler: TestUnionEventHandler<ItemEventDto>

    @BeforeEach
    fun cleanupMetaMocks() {
        clearMocks(ownershipClient)
        clearMocks(tokenClient)
        testEventHandlers.forEach { it.events.clear() }
    }

    fun findItemUpdates(itemId: String): List<ItemUpdateEventDto> {
        return testItemEventHandler.events.filterIsInstance(ItemUpdateEventDto::class.java)
            .filter { it.itemId.value == itemId }
    }

    fun findCollectionUpdates(collectionId: String): List<CollectionUpdateEventDto> {
        return testCollectionEventHandler.events.filterIsInstance(CollectionUpdateEventDto::class.java)
            .filter { it.collectionId.value == collectionId }
    }

    fun findItemDeletions(itemId: String): List<ItemDeleteEventDto> {
        return testItemEventHandler.events.filterIsInstance(ItemDeleteEventDto::class.java)
            .filter { it.itemId.value == itemId }
    }

    fun findOwnershipUpdates(ownershipId: String): List<OwnershipUpdateEventDto> {
        return testOwnershipEventHandler.events.filterIsInstance(OwnershipUpdateEventDto::class.java)
            .filter { it.ownershipId.value == ownershipId }
    }

    fun findOwnershipDeletions(ownershipId: String): List<OwnershipDeleteEventDto> {
        return testOwnershipEventHandler.events.filterIsInstance(OwnershipDeleteEventDto::class.java)
            .filter { it.ownershipId.value == ownershipId }
    }

    fun findOrderUpdates(orderId: String): List<OrderUpdateEventDto> {
        return testOrderEventHandler.events.filterIsInstance(OrderUpdateEventDto::class.java)
            .filter { it.orderId.value == orderId }
    }

    fun <T : ActivityDto> findActivityUpdates(
        id: String,
        type: Class<T>
    ): List<T> {
        return testActivityEventHandler.events.filterIsInstance(type)
            .filter { it.id.value == id }
    }

    suspend fun waitAssert(runnable: suspend () -> Unit) = WaitAssert.wait(runnable = runnable)
}
