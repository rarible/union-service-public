package com.rarible.protocol.nftorder.api.test

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.union.api.client.FixedUnionApiServiceUriProvider
import com.rarible.protocol.union.api.client.TestControllerApi
import com.rarible.protocol.union.api.client.UnionApiClientFactory
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
    fun testUnionTestControllerApi(unionApiClientFactory: UnionApiClientFactory): TestControllerApi {
        return unionApiClientFactory.createTestUnionApiClient()
    }
}