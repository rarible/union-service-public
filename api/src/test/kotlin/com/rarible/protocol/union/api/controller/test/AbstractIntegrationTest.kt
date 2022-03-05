package com.rarible.protocol.union.api.controller.test

import com.rarible.core.common.nowMillis
import com.rarible.core.kafka.KafkaMessage
import com.rarible.core.kafka.KafkaSendResult
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
import com.rarible.protocol.nftorder.api.test.mock.EthAuctionControllerApiMock
import com.rarible.protocol.nftorder.api.test.mock.EthItemControllerApiMock
import com.rarible.protocol.nftorder.api.test.mock.EthOrderControllerApiMock
import com.rarible.protocol.order.api.client.AuctionActivityControllerApi
import com.rarible.protocol.order.api.client.OrderActivityControllerApi
import com.rarible.protocol.union.api.controller.test.mock.eth.EthActivityControllerApiMock
import com.rarible.protocol.union.api.controller.test.mock.eth.EthOwnershipControllerApiMock
import com.rarible.protocol.union.api.controller.test.mock.flow.FlowItemControllerApiMock
import com.rarible.protocol.union.api.controller.test.mock.flow.FlowOrderControllerApiMock
import com.rarible.protocol.union.api.controller.test.mock.flow.FlowOwnershipControllerApiMock
import com.rarible.protocol.union.api.controller.test.mock.tezos.TezosItemControllerApiMock
import com.rarible.protocol.union.api.controller.test.mock.tezos.TezosOrderControllerApiMock
import com.rarible.protocol.union.api.controller.test.mock.tezos.TezosOwnershipControllerApiMock
import com.rarible.protocol.union.dto.ItemEventDto
import com.rarible.protocol.union.dto.OwnershipEventDto
import com.rarible.protocol.union.enrichment.meta.UnionMetaLoader
import io.mockk.clearMocks
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.web.client.RestTemplate
import reactor.kotlin.core.publisher.toMono
import java.math.BigDecimal
import java.net.URI
import java.util.*

@FlowPreview
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
    @Qualifier("test.union.meta.loader")
    lateinit var testUnionMetaLoader: UnionMetaLoader

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

    //--------------------- FLOW ---------------------//
    @Autowired
    lateinit var testFlowItemApi: FlowNftItemControllerApi

    @Autowired
    lateinit var testFlowOwnershipApi: FlowNftOwnershipControllerApi

    @Autowired
    lateinit var testFlowCollectionApi: FlowNftCollectionControllerApi

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

    lateinit var tezosItemControllerApiMock: TezosItemControllerApiMock
    lateinit var tezosOwnershipControllerApiMock: TezosOwnershipControllerApiMock
    lateinit var tezosOrderControllerApiMock: TezosOrderControllerApiMock

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
            testOwnershipEventProducer
        )
        ethereumItemControllerApiMock = EthItemControllerApiMock(testEthereumItemApi)
        ethereumOwnershipControllerApiMock = EthOwnershipControllerApiMock(testEthereumOwnershipApi)
        ethereumOrderControllerApiMock = EthOrderControllerApiMock(testEthereumOrderApi)
        ethereumAuctionControllerApiMock = EthAuctionControllerApiMock(testEthereumAuctionApi)
        ethereumActivityControllerApiMock = EthActivityControllerApiMock(testEthereumActivityItemApi)

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
            testCurrencyApi.getCurrencyRate(any(), any(), any())
        } returns CurrencyRateDto(
            date = nowMillis(),
            fromCurrencyId = UUID.randomUUID().toString(),
            rate = BigDecimal.ONE,
            toCurrencyId = UUID.randomUUID().toString()
        ).toMono()
    }
}
