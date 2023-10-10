package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.DomainResolveResultDto
import com.rarible.protocol.nft.api.client.NftDomainControllerApi
import com.rarible.protocol.union.dto.BlockchainDto
import io.mockk.coEvery
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import reactor.kotlin.core.publisher.toMono

@ExtendWith(MockKExtension::class)
class EthDomainServiceTest {

    private val blockchain = BlockchainDto.ETHEREUM

    @MockK
    private lateinit var nftDomainControllerApi: NftDomainControllerApi

    @InjectMockKs
    private lateinit var service: EthDomainService

    @Test
    fun `ethereum resolve`() = runBlocking<Unit> {
        val name = randomString()
        val ethResult = DomainResolveResultDto(randomString())
        coEvery { nftDomainControllerApi.resolveDomainByName(name) } returns ethResult.toMono()

        val result = service.resolve(name)
        assertThat(result.registrant).isEqualTo(ethResult.registrant)
    }
}
