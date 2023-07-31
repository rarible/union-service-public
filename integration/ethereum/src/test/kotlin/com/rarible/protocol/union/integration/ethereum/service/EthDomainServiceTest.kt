package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.DomainResolveResultDto
import com.rarible.protocol.nft.api.client.NftDomainControllerApi
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import reactor.kotlin.core.publisher.toMono

class EthDomainServiceTest {

    private val nftDomainControllerApi: NftDomainControllerApi = mockk()
    private val service = EthereumDomainService(nftDomainControllerApi)

    @Test
    fun `ethereum resolve`() = runBlocking<Unit> {
        val name = randomString()
        val ethResult = DomainResolveResultDto(randomString())
        coEvery { nftDomainControllerApi.resolveDomainByName(name) } returns ethResult.toMono()

        val result = service.resolve(name)
        assertThat(result.registrant).isEqualTo(ethResult.registrant)
    }
}
