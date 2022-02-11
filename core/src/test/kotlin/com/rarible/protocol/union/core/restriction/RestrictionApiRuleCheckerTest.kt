package com.rarible.protocol.union.core.restriction

import com.rarible.core.test.data.randomString
import com.rarible.protocol.union.api.ApiClient
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.core.model.RestrictionApiRule
import com.rarible.protocol.union.core.model.RestrictionCheckResult
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipRestrictionCheckFormDto
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import java.nio.charset.StandardCharsets

class RestrictionApiRuleCheckerTest {

    companion object {
        private val mockBackEnd = MockWebServer()
        private val mapper = ApiClient.createDefaultObjectMapper()
        private val webClient = WebClient.builder().build()

        @BeforeAll
        fun beforeAll() {
            mockBackEnd.start()
        }

        @AfterAll
        fun afterAll() {
            mockBackEnd.shutdown()
        }
    }

    val mockHost = "http://localhost:${mockBackEnd.port}"

    private val checker = RestrictionApiRuleChecker(webClient)

    @Test
    fun `get restriction`() = runBlocking<Unit> {
        val mockedResult = RestrictionCheckResult(true, null)
        mockBackEnd.enqueue(mockOkResponse(mockedResult))

        val itemId = ItemIdDto(BlockchainDto.ETHEREUM, randomString())
        val form = OwnershipRestrictionCheckFormDto(
            UnionAddressConverter.convert(BlockchainDto.ETHEREUM, "abc")
        )
        val rule = RestrictionApiRule(
            method = RestrictionApiRule.Method.GET,
            uriTemplate = "${mockHost}/\${itemId}?address=\${user}"
        )

        val checkResult = checker.checkRule(itemId, rule, form)

        val recordedRequest = mockBackEnd.takeRequest()

        assertThat(checkResult).isEqualTo(mockedResult)
        assertThat(recordedRequest.method).isEqualTo("GET")
        assertThat(recordedRequest.path).isEqualTo("/${itemId.value}?address=abc")
    }

    @Test
    fun `post restriction`() = runBlocking<Unit> {
        val mockedResult = RestrictionCheckResult(false, "error")
        mockBackEnd.enqueue(mockOkResponse(mockedResult))

        val itemId = ItemIdDto(BlockchainDto.ETHEREUM, randomString())
        val form = OwnershipRestrictionCheckFormDto(
            UnionAddressConverter.convert(BlockchainDto.ETHEREUM, "abc")
        )
        val rule = RestrictionApiRule(
            method = RestrictionApiRule.Method.POST,
            uriTemplate = "${mockHost}/check",
            bodyTemplate = "{\"itemId\":\"\${itemId}\", \"address\":\"\${user}\"}"
        )

        val checkResult = checker.checkRule(itemId, rule, form)

        val recordedRequest = mockBackEnd.takeRequest()
        val recordedBody = recordedRequest.body.readString(StandardCharsets.UTF_8)

        assertThat(checkResult).isEqualTo(mockedResult)
        assertThat(recordedRequest.method).isEqualTo("POST")
        assertThat(recordedRequest.path).isEqualTo("/check")
        assertThat(recordedBody).isEqualTo("""{"itemId":"${itemId.value}", "address":"abc"}""")
    }

    private fun mockOkResponse(result: RestrictionCheckResult): MockResponse {
        return MockResponse()
            .setBody(mapper.writeValueAsString(result))
            .setResponseCode(200)
            .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON.toString())
    }


}