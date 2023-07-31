package com.rarible.protocol.union.api.controller

import com.rarible.protocol.dto.DomainResolveResultDto
import com.rarible.protocol.nft.api.client.NftDomainControllerApi
import com.rarible.protocol.union.api.client.DomainControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono

@FlowPreview
@IntegrationTest
class DomainControllerFt : AbstractIntegrationTest() {

    @Autowired
    lateinit var domainControllerApi: DomainControllerApi

    @Autowired
    lateinit var nftDomainControllerApi: NftDomainControllerApi

    @Test
    fun `resolve - ok, ethereum`() = runBlocking<Unit> {
        val domain = "test.eth"

        val ethResult = DomainResolveResultDto(
            registrant = "0x0000000"
        )
        coEvery { nftDomainControllerApi.resolveDomainByName(domain) } returns ethResult.toMono()
        val result = domainControllerApi.resolve(domain).awaitFirst()

        assertThat(result.registrant).isEqualTo(ethResult.registrant)
    }

    @Test
    fun `resolve - false, domain not valid`() = runBlocking<Unit> {
        val domain = "test"

        val ex = assertThrows<DomainControllerApi.ErrorResolve> {
            domainControllerApi.resolve(domain).awaitFirst()
        }
        assertThat(ex.on400).isNotNull
    }

    @Test
    fun `resolve - false, top domain not found`() = runBlocking<Unit> {
        val domain = "test.io"

        val ex = assertThrows<DomainControllerApi.ErrorResolve> {
            domainControllerApi.resolve(domain).awaitFirst()
        }
        assertThat(ex.on404).isNotNull
    }
}
