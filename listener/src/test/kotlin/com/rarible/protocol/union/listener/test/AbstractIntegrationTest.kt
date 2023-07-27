package com.rarible.protocol.union.listener.test

import com.rarible.core.content.meta.loader.ContentMetaReceiver
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.union.core.test.TestUnionEventHandler
import com.rarible.protocol.union.core.test.WaitAssert
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
import com.rarible.protocol.union.integration.ethereum.mock.EthAuctionControllerApiMock
import com.rarible.protocol.union.integration.ethereum.mock.EthItemControllerApiMock
import com.rarible.protocol.union.integration.ethereum.mock.EthOrderControllerApiMock
import com.rarible.protocol.union.integration.ethereum.mock.EthOwnershipControllerApiMock
import io.mockk.clearMocks
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier

@Suppress("UNCHECKED_CAST")
abstract class AbstractIntegrationTest {

    @Autowired
    @Qualifier("test.content.meta.receiver")
    lateinit var testContentMetaReceiver: ContentMetaReceiver

    // --------------------- ETHEREUM ---------------------//
    @Autowired
    @Qualifier("ethereum.item.api")
    lateinit var testEthereumItemApi: NftItemControllerApi

    @Autowired
    @Qualifier("ethereum.collection.api")
    lateinit var testEthereumCollectionApi: NftCollectionControllerApi

    @Autowired
    @Qualifier("ethereum.ownership.api")
    lateinit var testEthereumOwnershipApi: NftOwnershipControllerApi

    @Autowired
    @Qualifier("ethereum.order.api")
    lateinit var testEthereumOrderApi: com.rarible.protocol.order.api.client.OrderControllerApi

    @Autowired
    @Qualifier("ethereum.auction.api")
    lateinit var testEthereumAuctionApi: com.rarible.protocol.order.api.client.AuctionControllerApi

    lateinit var ethereumItemControllerApiMock: EthItemControllerApiMock
    lateinit var ethereumOwnershipControllerApiMock: EthOwnershipControllerApiMock
    lateinit var ethereumOrderControllerApiMock: EthOrderControllerApiMock
    lateinit var ethereumAuctionControllerApiMock: EthAuctionControllerApiMock

    @Autowired
    lateinit var ethItemProducer: RaribleKafkaProducer<com.rarible.protocol.dto.NftItemEventDto>

    @Autowired
    lateinit var ethOwnershipProducer: RaribleKafkaProducer<com.rarible.protocol.dto.NftOwnershipEventDto>

    @Autowired
    lateinit var ethOrderProducer: RaribleKafkaProducer<com.rarible.protocol.dto.OrderEventDto>

    @Autowired
    lateinit var ethActivityProducer: RaribleKafkaProducer<com.rarible.protocol.dto.ActivityDto>

    @Autowired
    lateinit var testEventHandlers: List<TestUnionEventHandler<*>>

    @Autowired
    lateinit var testCollectionEventHandler: TestUnionEventHandler<CollectionEventDto>

    @Autowired
    lateinit var testItemEventHandler: TestUnionEventHandler<ItemEventDto>

    @Autowired
    lateinit var testOwnershipEventHandler: TestUnionEventHandler<OwnershipEventDto>

    @Autowired
    lateinit var testOrderEventHandler: TestUnionEventHandler<OrderEventDto>

    @Autowired
    lateinit var testActivityEventHandler: TestUnionEventHandler<ActivityDto>

    @BeforeEach
    fun beforeEachTest() {
        clearMocks(
            testEthereumItemApi,
            testEthereumOwnershipApi,
            testEthereumOrderApi,
            testEthereumAuctionApi,
            testContentMetaReceiver
        )
        ethereumItemControllerApiMock = EthItemControllerApiMock(testEthereumItemApi)
        ethereumOwnershipControllerApiMock = EthOwnershipControllerApiMock(testEthereumOwnershipApi)
        ethereumOrderControllerApiMock = EthOrderControllerApiMock(testEthereumOrderApi)
        ethereumAuctionControllerApiMock = EthAuctionControllerApiMock(testEthereumAuctionApi)
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
