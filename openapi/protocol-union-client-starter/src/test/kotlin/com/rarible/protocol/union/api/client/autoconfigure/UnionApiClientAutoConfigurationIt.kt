package com.rarible.protocol.union.api.client.autoconfigure

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.union.api.client.UnionApiClientFactory
import com.rarible.protocol.union.api.client.UnionApiServiceUriProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.SpringBootConfiguration
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@SpringBootTest
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(UnionApiClientAutoConfigurationIt.Configuration::class)
class UnionApiClientAutoConfigurationIt {

    @Autowired
    private lateinit var unionApiServiceUriProvider: UnionApiServiceUriProvider

    @Autowired
    private lateinit var unionApiClientFactory: UnionApiClientFactory

    @Autowired
    @Qualifier(UNION_WEB_CLIENT_CUSTOMIZER)
    private lateinit var webClientCustomizer: WebClientCustomizer

    @Test
    fun `test default clients initialized`() {
        assertThat(unionApiServiceUriProvider).isNotNull
        assertThat(unionApiClientFactory).isNotNull

        every { webClientCustomizer.customize(any()) } returns Unit

        unionApiClientFactory.createActivityApiClient()

        verify { webClientCustomizer.customize(any()) }
    }

    @Test
    fun `test default client uri`() {
        val uri = unionApiServiceUriProvider.getUri()
        assertThat(uri.toString()).isEqualTo("http://test-union-api:8080")
    }

    @TestConfiguration
    internal class Configuration {

        @Bean
        @Qualifier(UNION_WEB_CLIENT_CUSTOMIZER)
        fun webClientCustomizer(): WebClientCustomizer {
            return mockk()
        }

        @Bean
        fun applicationEnvironmentInfo(): ApplicationEnvironmentInfo {
            return ApplicationEnvironmentInfo("test", "test.com")
        }
    }
}
