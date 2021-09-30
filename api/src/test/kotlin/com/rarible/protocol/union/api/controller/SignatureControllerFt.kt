package com.rarible.protocol.union.api.controller

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBinary
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.EthereumSignatureValidationFormDto
import com.rarible.protocol.union.api.client.SignatureControllerApi
import com.rarible.protocol.union.api.controller.test.AbstractIntegrationTest
import com.rarible.protocol.union.api.controller.test.IntegrationTest
import com.rarible.protocol.union.core.converter.UnionAddressConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.SignatureValidationFormDto
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

    @Autowired
    lateinit var signatureControllerApi: SignatureControllerApi

    @Test
    fun `validate signature - ethereum`() = runBlocking<Unit> {
        val ethForm = EthereumSignatureValidationFormDto(randomAddress(), randomString(), randomBinary())

        val unionForm = SignatureValidationFormDto(
            signer = UnionAddressConverter.convert(ethForm.signer, BlockchainDto.ETHEREUM),
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

        val unionForm = SignatureValidationFormDto(
            signer = UnionAddressConverter.convert(ethForm.signer, BlockchainDto.POLYGON),
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
