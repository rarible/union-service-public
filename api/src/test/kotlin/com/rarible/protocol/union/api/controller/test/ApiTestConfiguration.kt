package com.rarible.protocol.union.api.controller.test

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.union.api.client.*
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Lazy
import org.springframework.context.annotation.Primary
import java.net.URI

@Lazy
@Configuration
class ApiTestConfiguration {

    @Bean
    fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
        return ApplicationEnvironmentInfo("test", "test.com")
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
    fun testUnionCollectionControllerApi(unionApiClientFactory: UnionApiClientFactory): CollectionControllerApi {
        return unionApiClientFactory.createCollectionApiClient()
    }

    @Bean
    fun testUnionActivityControllerApi(unionApiClientFactory: UnionApiClientFactory): ActivityControllerApi {
        return unionApiClientFactory.createActivityApiClient()
    }
}