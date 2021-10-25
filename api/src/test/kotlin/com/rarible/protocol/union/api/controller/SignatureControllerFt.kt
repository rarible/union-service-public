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
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
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
            signer = EthConverter.convert(ethForm.signer, BlockchainDto.ETHEREUM),
            message = ethForm.message,
            signature = ethForm.signature.prefixed()
        )

        coEvery { testEthereumSignatureApi.validate(ethForm) } returns true.toMono()
        val result = signatureControllerApi.validate(unionForm).awaitFirst()

        assertThat(result).isEqualTo(true)
    }

    @Test
    fun `validate signature - tezos`() = runBlocking<Unit> {
        val tezosForm = com.rarible.protocol.tezos.dto.SignatureValidationFormDto(
            randomString(),
            randomString(),
            randomString()
        )

        val unionForm = SignatureValidationFormDto(
            signer = UnionAddressConverter.convert(tezosForm.signer, BlockchainDto.TEZOS),
            message = tezosForm.message,
            signature = tezosForm.signature
        )

        coEvery { testTezosSignatureApi.validate(tezosForm) } returns false.toMono()
        val result = signatureControllerApi.validate(unionForm).awaitFirst()

        assertThat(result).isEqualTo(false)
    }

    @Test
    fun `validate signature - flow`() = runBlocking<Unit> {
        val unionForm = SignatureValidationFormDto(
            signer = UnionAddressConverter.convert(randomString(), BlockchainDto.FLOW),
            publicKey = randomString(),
            message = randomString(),
            signature = randomString()
        )

        coEvery {
            testFlowSignatureApi.verifySignature(
                unionForm.publicKey,
                unionForm.signer.value,
                unionForm.signature,
                unionForm.message
            )
        } returns true.toMono()
        val result = signatureControllerApi.validate(unionForm).awaitFirst()

        assertThat(result).isEqualTo(true)
    }
}
