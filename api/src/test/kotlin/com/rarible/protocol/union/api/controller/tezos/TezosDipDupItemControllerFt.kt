package com.rarible.protocol.union.api.controller.tezos

import com.rarible.dipdup.client.exception.DipDupNotFound
import com.rarible.protocol.union.api.client.ItemControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.integration.tezos.data.randomTezosItemId
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate

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
        "logging.logjson.enabled = false",
        "integration.tezos.dipdup.useDipDupTokens = true"
    ]
)
class TezosDipDupItemControllerFt : AbstractIntegrationTest() {

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var testTemplate: RestTemplate

    @Autowired
    lateinit var itemControllerClient: ItemControllerApi

    private fun baseUrl(): String {
        return "http://localhost:$port/v0.1"
    }

    @Test
    fun `should return 404 on non-existed item`() = runBlocking<Unit> {
        val tezosItemId = randomTezosItemId()
        val url = "${baseUrl()}/items/${tezosItemId.fullId()}"

        coEvery {
            dipdupTokenClient.getTokenById(any())
        } throws DipDupNotFound("!")

        assertThrows<HttpClientErrorException.NotFound> {
            runBlocking {
                testTemplate.getForEntity(url, UnionCollection::class.java)
            }
        }
    }
}
