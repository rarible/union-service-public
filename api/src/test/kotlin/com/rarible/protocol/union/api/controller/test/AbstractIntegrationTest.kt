package com.rarible.protocol.union.api.controller.test

import com.rarible.core.common.nowMillis
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.KafkaSendResult
import com.rarible.core.kafka.RaribleKafkaConsumer
import com.rarible.core.kafka.RaribleKafkaProducer
import com.rarible.protocol.currency.api.client.CurrencyControllerApi
import com.rarible.protocol.currency.dto.CurrencyRateDto
import com.rarible.protocol.flow.nft.api.client.FlowNftCollectionControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftCryptoControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftItemControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftOrderActivityControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowNftOwnershipControllerApi
import com.rarible.protocol.flow.nft.api.client.FlowOrderControllerApi
import com.rarible.protocol.nft.api.client.NftActivityControllerApi
import com.rarible.protocol.nft.api.client.NftCollectionControllerApi
import com.rarible.protocol.nft.api.client.NftItemControllerApi
import com.rarible.protocol.nft.api.client.NftOwnershipControllerApi
import com.rarible.protocol.order.api.client.AuctionActivityControllerApi
import com.rarible.protocol.order.api.client.OrderActivityControllerApi
import com.rarible.protocol.union.dto.CollectionEventDto
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.enrichment.meta.item.ItemMetaLoader
import com.rarible.protocol.union.integration.ethereum.mock.EthActivityControllerApiMock
import com.rarible.protocol.union.integration.ethereum.mock.EthAuctionControllerApiMock
import com.rarible.protocol.union.integration.ethereum.mock.EthItemControllerApiMock
import com.rarible.protocol.union.integration.ethereum.mock.EthOrderControllerApiMock
import com.rarible.protocol.union.integration.ethereum.mock.EthOwnershipControllerApiMock
import com.rarible.protocol.union.integration.flow.mock.FlowItemControllerApiMock
import com.rarible.protocol.union.integration.flow.mock.FlowOrderControllerApiMock
import com.rarible.protocol.union.integration.flow.mock.FlowOwnershipControllerApiMock
import com.rarible.protocol.union.integration.tezos.mock.TezosItemControllerApiMock
import com.rarible.protocol.union.integration.tezos.mock.TezosOrderControllerApiMock
import com.rarible.protocol.union.integration.tezos.mock.TezosOwnershipControllerApiMock
import io.mockk.clearMocks
import io.mockk.coEvery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.client.RestTemplate
import reactor.kotlin.core.publisher.toMono
import java.math.BigDecimal
import java.net.URI
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import com.rarible.protocol.solana.api.client.ActivityControllerApi as SolanaActivityControllerApi
import com.rarible.protocol.solana.api.client.CollectionControllerApi as SolanaCollectionControllerApi

abstract class AbstractIntegrationTest {

    @Autowired
    @Qualifier("testLocalhostUri")
    protected lateinit var baseUri: URI

    @Autowired
    protected lateinit var testRestTemplate: RestTemplate

    @Autowired
    protected lateinit var testItemEventProducer: RaribleKafkaProducer<ItemEventDto>

    @Autowired
    protected lateinit var testOwnershipEventProducer: RaribleKafkaProducer<OwnershipEventDto>

    @Autowired
    protected lateinit var testCollectionEventProducer: RaribleKafkaProducer<CollectionEventDto>

    @Autowired
    @Qualifier("test.union.meta.loader")
    lateinit var testItemMetaLoader: ItemMetaLoader

    //--------------------- CURRENCY ---------------------//

    @Autowired
    lateinit var testCurrencyApi: CurrencyControllerApi

    //--------------------- ETHEREUM ---------------------//
    @Autowired
    @Qualifier("ethereum.item.api")
    lateinit var testEthereumItemApi: NftItemControllerApi

    @Autowired
    @Qualifier("ethereum.ownership.api")
    lateinit var testEthereumOwnershipApi: NftOwnershipControllerApi

    @Autowired
    @Qualifier("ethereum.collection.api")
    lateinit var testEthereumCollectionApi: NftCollectionControllerApi

    @Autowired
    @Qualifier("ethereum.order.api")
    lateinit var testEthereumOrderApi: com.rarible.protocol.order.api.client.OrderControllerApi

    @Autowired
    @Qualifier("ethereum.auction.api")
    lateinit var testEthereumAuctionApi: com.rarible.protocol.order.api.client.AuctionControllerApi

    @Autowired
    @Qualifier("ethereum.signature.api")
    lateinit var testEthereumSignatureApi: com.rarible.protocol.order.api.client.OrderSignatureControllerApi

    @Autowired
    @Qualifier("ethereum.activity.api.item")
    lateinit var testEthereumActivityItemApi: NftActivityControllerApi

    @Autowired
    @Qualifier("ethereum.activity.api.order")
    lateinit var testEthereumActivityOrderApi: OrderActivityControllerApi

    @Autowired
    @Qualifier("ethereum.activity.api.auction")
    lateinit var testEthereumActivityAuctionApi: AuctionActivityControllerApi

    lateinit var ethereumItemControllerApiMock: EthItemControllerApiMock
    lateinit var ethereumOwnershipControllerApiMock: EthOwnershipControllerApiMock
    lateinit var ethereumOrderControllerApiMock: EthOrderControllerApiMock
    lateinit var ethereumAuctionControllerApiMock: EthAuctionControllerApiMock
    lateinit var ethereumActivityControllerApiMock: EthActivityControllerApiMock

    //--------------------- POLYGON ---------------------//
    @Autowired
    @Qualifier("polygon.item.api")
    lateinit var testPolygonItemApi: NftItemControllerApi

    @Autowired
    @Qualifier("polygon.ownership.api")
    lateinit var testPolygonOwnershipApi: NftOwnershipControllerApi

    @Autowired
    @Qualifier("polygon.collection.api")
    lateinit var testPolygonCollectionApi: NftCollectionControllerApi

    @Autowired
    @Qualifier("polygon.order.api")
    lateinit var testPolygonOrderApi: com.rarible.protocol.order.api.client.OrderControllerApi

    @Autowired
    @Qualifier("polygon.auction.api")
    lateinit var testPolygonAuctionApi: com.rarible.protocol.order.api.client.AuctionControllerApi

    @Autowired
    @Qualifier("polygon.signature.api")
    lateinit var testPolygonSignatureApi: com.rarible.protocol.order.api.client.OrderSignatureControllerApi

    @Autowired
    @Qualifier("polygon.activity.api.item")
    lateinit var testPolygonActivityItemApi: NftActivityControllerApi

    @Autowired
    @Qualifier("polygon.activity.api.order")
    lateinit var testPolygonActivityOrderApi: OrderActivityControllerApi

    @Autowired
    @Qualifier("polygon.activity.api.auction")
    lateinit var testPolygonActivityAuctionApi: AuctionActivityControllerApi

    lateinit var polygonItemControllerApiMock: EthItemControllerApiMock
    lateinit var polygonOwnershipControllerApiMock: EthOwnershipControllerApiMock
    lateinit var polygonOrderControllerApiMock: EthOrderControllerApiMock
    lateinit var polygonAuctionControllerApiMock: EthAuctionControllerApiMock

    //--------------------- SOLANA ---------------------//

    @Autowired
    lateinit var testSolanaActivityApi: SolanaActivityControllerApi

    //--------------------- FLOW ---------------------//
    @Autowired
    lateinit var testFlowItemApi: FlowNftItemControllerApi

    @Autowired
    lateinit var testFlowOwnershipApi: FlowNftOwnershipControllerApi

    @Autowired
    lateinit var testFlowCollectionApi: FlowNftCollectionControllerApi

    @Autowired
    lateinit var testSolanaCollectionApi: SolanaCollectionControllerApi

    @Autowired
    lateinit var testFlowOrderApi: FlowOrderControllerApi

    @Autowired
    lateinit var testFlowSignatureApi: FlowNftCryptoControllerApi

    @Autowired
    lateinit var testFlowActivityApi: FlowNftOrderActivityControllerApi

    lateinit var flowItemControllerApiMock: FlowItemControllerApiMock
    lateinit var flowOwnershipControllerApiMock: FlowOwnershipControllerApiMock
    lateinit var flowOrderControllerApiMock: FlowOrderControllerApiMock

    //--------------------- TEZOS ---------------------//
    @Autowired
    lateinit var testTezosItemApi: com.rarible.protocol.tezos.api.client.NftItemControllerApi

    @Autowired
    lateinit var testTezosOwnershipApi: com.rarible.protocol.tezos.api.client.NftOwnershipControllerApi

    @Autowired
    lateinit var testTezosCollectionApi: com.rarible.protocol.tezos.api.client.NftCollectionControllerApi

    @Autowired
    lateinit var testTezosOrderApi: com.rarible.protocol.tezos.api.client.OrderControllerApi

    @Autowired
    lateinit var testTezosSignatureApi: com.rarible.protocol.tezos.api.client.OrderSignatureControllerApi

    @Autowired
    lateinit var testDipDupOrderClient: com.rarible.dipdup.client.OrderClient

    @Autowired
    lateinit var tzktTokenClient: com.rarible.tzkt.client.TokenClient

    @Autowired
    lateinit var tzktCollectionClient: com.rarible.tzkt.client.CollectionClient

    @Autowired
    lateinit var tzktOwnershipClient: com.rarible.tzkt.client.OwnershipClient

    lateinit var tezosItemControllerApiMock: TezosItemControllerApiMock
    lateinit var tezosOwnershipControllerApiMock: TezosOwnershipControllerApiMock
    lateinit var tezosOrderControllerApiMock: TezosOrderControllerApiMock

    @Autowired
    lateinit var itemConsumer: RaribleKafkaConsumer<ItemEventDto>
    var itemEvents: Queue<KafkaMessage<ItemEventDto>>? = null
    private var itemJob: Deferred<Unit>? = null

    @Autowired
    lateinit var ownershipConsumer: RaribleKafkaConsumer<OwnershipEventDto>
    var ownershipEvents: Queue<KafkaMessage<OwnershipEventDto>>? = null
    private var ownershipJob: Deferred<Unit>? = null

    @Autowired
    @Qualifier("item.producer.api")
    lateinit var itemProducer: RaribleKafkaProducer<ItemEventDto>

    @Autowired
    lateinit var ethOwnershipProducer: RaribleKafkaProducer<ItemEventDto>

    @BeforeEach
    fun beforeEach() {
        clearMocks(
            testEthereumItemApi,
            testEthereumOwnershipApi,
            testEthereumOrderApi,

            testPolygonItemApi,
            testPolygonOwnershipApi,
            testPolygonOrderApi,

            testFlowItemApi,
            testFlowOwnershipApi,
            testFlowOrderApi,

            testItemEventProducer,
            testOwnershipEventProducer,

            testItemMetaLoader
        )
        ethereumItemControllerApiMock = EthItemControllerApiMock(testEthereumItemApi)
        ethereumOwnershipControllerApiMock = EthOwnershipControllerApiMock(testEthereumOwnershipApi)
        ethereumOrderControllerApiMock = EthOrderControllerApiMock(testEthereumOrderApi)
        ethereumAuctionControllerApiMock = EthAuctionControllerApiMock(testEthereumAuctionApi)
        ethereumActivityControllerApiMock = EthActivityControllerApiMock(
            testEthereumActivityItemApi, testEthereumActivityOrderApi
        )

        polygonItemControllerApiMock = EthItemControllerApiMock(testPolygonItemApi)
        polygonOwnershipControllerApiMock = EthOwnershipControllerApiMock(testPolygonOwnershipApi)
        polygonOrderControllerApiMock = EthOrderControllerApiMock(testPolygonOrderApi)
        polygonAuctionControllerApiMock = EthAuctionControllerApiMock(testPolygonAuctionApi)

        flowItemControllerApiMock = FlowItemControllerApiMock(testFlowItemApi)
        flowOwnershipControllerApiMock = FlowOwnershipControllerApiMock(testFlowOwnershipApi)
        flowOrderControllerApiMock = FlowOrderControllerApiMock(testFlowOrderApi)

        tezosItemControllerApiMock = TezosItemControllerApiMock(testTezosItemApi)
        tezosOwnershipControllerApiMock = TezosOwnershipControllerApiMock(testTezosOwnershipApi)
        tezosOrderControllerApiMock = TezosOrderControllerApiMock(testTezosOrderApi)

        coEvery {
            testItemEventProducer.send(any() as KafkaMessage<ItemEventDto>)
        } returns KafkaSendResult.Success("")

        coEvery {
            testOwnershipEventProducer.send(any() as KafkaMessage<OwnershipEventDto>)
        } returns KafkaSendResult.Success("")

        coEvery {
            testCollectionEventProducer.send(any() as KafkaMessage<CollectionEventDto>)
        } returns KafkaSendResult.Success("")

        coEvery {
            testCurrencyApi.getCurrencyRate(any(), any(), any())
        } returns CurrencyRateDto(
            date = nowMillis(),
            fromCurrencyId = UUID.randomUUID().toString(),
            rate = BigDecimal.ONE,
            toCurrencyId = UUID.randomUUID().toString()
        ).toMono()
    }

    fun <T> runWithKafka(block: suspend CoroutineScope.() -> T): T = runBlocking {

        ownershipEvents = LinkedBlockingQueue()
        ownershipJob = async { ownershipConsumer.receiveAutoAck().collect { ownershipEvents?.add(it) } }

        itemEvents = LinkedBlockingQueue()
        itemJob = async {
            itemConsumer.receiveAutoAck().collect {
                itemEvents?.add(it)
            }
        }


        val result = try {
            block()
        } finally {
            itemJob?.cancelAndJoin()
            ownershipJob?.cancelAndJoin()
        }
        result
    }
}
