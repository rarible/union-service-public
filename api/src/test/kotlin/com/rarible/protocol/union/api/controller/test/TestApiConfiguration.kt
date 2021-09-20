package com.rarible.protocol.union.api.controller.test

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.flow.nft.api.client.*
import com.rarible.protocol.nft.api.client.*
import com.rarible.protocol.order.api.client.OrderActivityControllerApi
import com.rarible.protocol.order.api.client.OrderIndexerApiClientFactory
import com.rarible.protocol.union.api.client.*
import io.mockk.mockk
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary
import org.springframework.web.client.RestTemplate
import java.net.URI

@Lazy
@Configuration
class TestApiConfiguration {

    @Bean
    fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
        return ApplicationEnvironmentInfo("test", "test.com")
    }

    @Bean
    fun testRestTemplate(): RestTemplate {
        return RestTemplate()
    }

    @Bean
    @Primary
    fun testUnionApiClientFactory(@LocalServerPort port: Int): UnionApiClientFactory {
        return UnionApiClientFactory(FixedUnionApiServiceUriProvider(URI("http://localhost:${port}")))
    }

    @Bean
    fun testUnionItemControllerApi(unionApiClientFactory: UnionApiClientFactory): ItemControllerApi {
        return unionApiClientFactory.createItemApiClient()
    }

    @Bean
    fun testUnionOwnershipControllerApi(unionApiClientFactory: UnionApiClientFactory): OwnershipControllerApi {
        return unionApiClientFactory.createOwnershipApiClient()
    }

    @Bean
    fun testUnionOrderControllerApi(unionApiClientFactory: UnionApiClientFactory): OrderControllerApi {
        return unionApiClientFactory.createOrderApiClient()
    }

    @Bean
    fun testUnionSignatureControllerApi(unionApiClientFactory: UnionApiClientFactory): SignatureControllerApi {
        return unionApiClientFactory.createSignatureApiClient()
    }

    @Bean
    fun testUnionCollectionControllerApi(unionApiClientFactory: UnionApiClientFactory): CollectionControllerApi {
        return unionApiClientFactory.createCollectionApiClient()
    }

    @Bean
    fun testUnionActivityControllerApi(unionApiClientFactory: UnionApiClientFactory): ActivityControllerApi {
        return unionApiClientFactory.createActivityApiClient()
    }

    //--------------------- ETHEREUM ---------------------//
    @Bean
    @Primary
    @Qualifier("ethereum.item.api")
    fun testEthereumItemApi(): NftItemControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("ethereum.ownership.api")
    fun testEthereumOwnershipApi(): NftOwnershipControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("ethereum.collection.api")
    fun testEthereumCollectionApi(): NftCollectionControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("ethereum.order.api")
    fun testEthereumOrderApi(): com.rarible.protocol.order.api.client.OrderControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("ethereum.signature.api")
    fun testEthereumSignatureApi(): com.rarible.protocol.order.api.client.OrderSignatureControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("ethereum.activity.api.item")
    fun testEthereumActivityItemApi(factory: NftIndexerApiClientFactory): NftActivityControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("ethereum.activity.api.order")
    fun testEthereumActivityOrderApi(factory: OrderIndexerApiClientFactory): OrderActivityControllerApi = mockk()

    //--------------------- POLYGON ---------------------//
    @Bean
    @Primary
    @Qualifier("polygon.item.api")
    fun testPolygonNftItemApi(): NftItemControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("polygon.ownership.api")
    fun testPolygonNftOwnershipApi(): NftOwnershipControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("polygon.collection.api")
    fun testPolygonNftCollectionApi(): NftCollectionControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("polygon.order.api")
    fun testPolygonOrderApi(): com.rarible.protocol.order.api.client.OrderControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("polygon.signature.api")
    fun testPolygonSignatureApi(): com.rarible.protocol.order.api.client.OrderSignatureControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("polygon.activity.api.item")
    fun testPolygonActivityItemApi(factory: NftIndexerApiClientFactory): NftActivityControllerApi = mockk()

    @Bean
    @Primary
    @Qualifier("polygon.activity.api.order")
    fun testPolygonActivityOrderApi(factory: OrderIndexerApiClientFactory): OrderActivityControllerApi = mockk()

    //--------------------- FLOW ---------------------//
    @Bean
    @Primary
    fun testFlowItemApi(): FlowNftItemControllerApi = mockk()

    @Bean
    @Primary
    fun testFlowOwnershipApi(): FlowNftOwnershipControllerApi = mockk()

    @Bean
    @Primary
    fun testFlowCollectionApi(): FlowNftCollectionControllerApi = mockk()

    @Bean
    @Primary
    fun testFlowOrderApi(): FlowOrderControllerApi = mockk()

    @Bean
    @Primary
    fun testFlowActivityApi(): FlowNftOrderActivityControllerApi = mockk()

}
