package com.rarible.protocol.union.api.controller

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBinary
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.EthereumSignatureValidationFormDto
import com.rarible.protocol.union.api.client.SignatureControllerApi
import com.rarible.protocol.union.api.configuration.PageSize
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.ethereum.converter.EthAddressConverter
import com.rarible.protocol.union.dto.EthBlockchainDto
import com.rarible.protocol.union.dto.UnionSignatureValidationFormDto
import io.mockk.coEvery
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import reactor.kotlin.core.publisher.toMono

@FlowPreview
@IntegrationTest
class SignatureControllerFt : AbstractIntegrationTest() {

    private val continuation = null
    private val size = PageSize.OWNERSHIP.default

    @Autowired
    lateinit var signatureControllerApi: SignatureControllerApi

    @Test
    fun `validate signature - ethereum`() = runBlocking<Unit> {
        val ethForm = EthereumSignatureValidationFormDto(randomAddress(), randomString(), randomBinary())

        val unionForm = UnionSignatureValidationFormDto(
            signer = EthAddressConverter.convert(ethForm.signer, EthBlockchainDto.ETHEREUM).fullId(),
            message = ethForm.message,
            signature = ethForm.signature.prefixed()
        )

        coEvery { testEthereumSignatureApi.validate(ethForm) } returns true.toMono()
        val result = signatureControllerApi.validate(unionForm).awaitFirst()

        assertThat(result).isEqualTo(true)
    }

    @Test
    fun `validate signature - polygon`() = runBlocking<Unit> {
        val ethForm = EthereumSignatureValidationFormDto(randomAddress(), randomString(), randomBinary())

        val unionForm = UnionSignatureValidationFormDto(
            signer = EthAddressConverter.convert(ethForm.signer, EthBlockchainDto.POLYGON).fullId(),
            message = ethForm.message,
            signature = ethForm.signature.prefixed()
        )

        coEvery { testPolygonSignatureApi.validate(ethForm) } returns false.toMono()
        val result = signatureControllerApi.validate(unionForm).awaitFirst()

        assertThat(result).isEqualTo(false)
    }

    @Test
    fun `validate signature - flow`() = runBlocking<Unit> {
        // TODO implement
    }

}
