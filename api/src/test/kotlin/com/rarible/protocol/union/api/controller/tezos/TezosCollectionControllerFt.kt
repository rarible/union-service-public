package com.rarible.protocol.union.api.controller.tezos

import com.rarible.protocol.union.api.client.CollectionControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.model.TokenId
import com.rarible.protocol.union.core.model.UnionCollection
import com.rarible.protocol.union.dto.CollectionDto
import com.rarible.protocol.union.integration.flow.data.randomFlowAddress
import com.rarible.protocol.union.integration.tezos.data.randomTezosAddress
import com.rarible.protocol.union.integration.tezos.data.randomTzktContract
import com.rarible.protocol.union.integration.tezos.entity.TezosCollection
import com.rarible.protocol.union.integration.tezos.entity.TezosCollectionRepository
import com.rarible.tzkt.model.CollectionType
import com.rarible.tzkt.model.TzktBadRequest
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.web.server.LocalServerPort
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestTemplate
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.math.BigInteger

@FlowPreview
@IntegrationTest
class TezosCollectionControllerFt : AbstractIntegrationTest() {

    @LocalServerPort
    var port: Int = 0

    @Autowired
    lateinit var testTemplate: RestTemplate

    @Autowired
    lateinit var tezosCollectionRepository: TezosCollectionRepository

    @Autowired
    lateinit var collectionControllerClient: CollectionControllerApi

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
    fun `should return 400 on user address`() = runBlocking<Unit> {
        val userAddress = randomTezosAddress()
        val url = "${baseUrl()}/collections/${userAddress.value}"

        coEvery {
            tzktCollectionClient.collection(userAddress.value)
        } throws TzktBadRequest(WebClientResponseException(400, "", null, null, null))

        assertThrows<HttpClientErrorException.BadRequest> {
            runBlocking {
                testTemplate.getForEntity(url, UnionCollection::class.java)
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

    @Test
    fun `should return collection with type`() = runBlocking<Unit> {
        val collectionId = randomTezosAddress()

        coEvery {
            tzktCollectionClient.collection(collectionId.value)
        } returns randomTzktContract(collectionId.value)
        coEvery {
            tzktCollectionClient.collectionType(collectionId.value)
        } returns CollectionType.NFT

        val collection = collectionControllerClient.getCollectionById(collectionId.fullId()).awaitSingle()
        assertThat(collection.type).isEqualTo(CollectionDto.Type.TEZOS_NFT)
    }

    @Test
    fun `should return collection with cached type`() = runBlocking<Unit> {
        val collectionId = randomTezosAddress()

        coEvery {
            tzktCollectionClient.collection(collectionId.value)
        } returns randomTzktContract(collectionId.value)
        tezosCollectionRepository.adjustCollectionType(collectionId.value, TezosCollection.Type.NFT)

        val collection = collectionControllerClient.getCollectionById(collectionId.fullId()).awaitSingle()
        assertThat(collection.type).isEqualTo(CollectionDto.Type.TEZOS_NFT)
    }

}
