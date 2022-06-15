package com.rarible.protocol.union.api.controller.tezos

import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.model.TokenId
import com.rarible.protocol.union.integration.tezos.data.randomTezosAddress
import com.rarible.protocol.union.integration.flow.data.randomFlowAddress
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import java.math.BigInteger

@FlowPreview
@IntegrationTest
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "application.environment = test",
        "spring.cloud.consul.config.enabled = false",
        "spring.cloud.service-registry.auto-registration.enabled = false",
        "spring.cloud.discovery.enabled = false",
        "logging.logstash.tcp-socket.enabled = false",
        "integration.tezos.dipdup.enabled = true" // turn on dipdup integration
    ]
)
class TezosCollectionControllerFt : AbstractIntegrationTest() {

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var testTemplate: RestTemplate

    private fun baseUrl(): String {
        return "http://localhost:${port}/v0.1"
    }

    @Test
    fun `should generates token_ids for tezos`() = runBlocking<Unit> {
        val collectionId = randomTezosAddress()
        val url = "${baseUrl()}/collections/${collectionId.fullId()}/generate_token_id"

        coEvery {
            tzktTokenClient.tokenCount(collectionId.value)
        } returns BigInteger.ZERO

        testTemplate.getForEntity(url, TokenId::class.java).body.apply {
            assertThat(tokenId).isEqualTo("1")
        }
        testTemplate.getForEntity(url, TokenId::class.java).body.apply {
            assertThat(tokenId).isEqualTo("2")
        }
    }

    @Test
    fun `should adjust lastTokenId for tezos`() = runBlocking<Unit> {
        val collectionId = randomTezosAddress()
        val url = "${baseUrl()}/collections/${collectionId.fullId()}/generate_token_id"

        coEvery {
            tzktTokenClient.tokenCount(collectionId.value)
        } returns BigInteger.ONE

        testTemplate.getForEntity(url, TokenId::class.java).body.apply {
            assertThat(tokenId).isEqualTo("2")
        }
    }

    @Test
    fun `should return 400 on non-existent collection`() = runBlocking<Unit> {
        val collectionId = randomTezosAddress()
        val url = "${baseUrl()}/collections/${collectionId.fullId()}/generate_token_id"

        coEvery {
            tzktTokenClient.tokenCount(collectionId.value)
        } throws RuntimeException()

        assertThrows<HttpClientErrorException.BadRequest> {
            runBlocking {
                testTemplate.getForEntity(url, TokenId::class.java)
            }
        }
    }

    @Test
    fun `should return 400 on non-supporting blockchain`() = runBlocking<Unit> {
        val collectionId = randomFlowAddress()
        val url = "${baseUrl()}/collections/${collectionId.fullId()}/generate_token_id"

        assertThrows<HttpClientErrorException.BadRequest> {
            runBlocking {
                testTemplate.getForEntity(url, TokenId::class.java)
            }
        }
    }

}
