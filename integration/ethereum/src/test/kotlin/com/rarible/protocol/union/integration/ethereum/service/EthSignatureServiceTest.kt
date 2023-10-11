package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.core.test.data.randomAddress
import com.rarible.core.test.data.randomBigInt
import com.rarible.core.test.data.randomBinary
import com.rarible.core.test.data.randomString
import com.rarible.protocol.dto.EthereumSignatureValidationFormDto
import com.rarible.protocol.dto.SeaportFulfillmentSimpleResponseDto
import com.rarible.protocol.dto.X2Y2GetCancelInputRequestDto
import com.rarible.protocol.dto.X2Y2OrderSignRequestDto
import com.rarible.protocol.dto.X2Y2SignResponseDto
import com.rarible.protocol.order.api.client.OrderSignatureControllerApi
import com.rarible.protocol.union.core.exception.UnionException
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OpenSeaFillOrderSignatureInputFormDto
import com.rarible.protocol.union.dto.X2Y2CancelOrderSignatureInputFormDto
import com.rarible.protocol.union.dto.X2Y2FillOrderSignatureInputFormDto
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import reactor.kotlin.core.publisher.toMono

@ExtendWith(MockKExtension::class)
class EthSignatureServiceTest {

    private val blockchain = BlockchainDto.ETHEREUM

    @MockK
    private lateinit var signatureControllerApi: OrderSignatureControllerApi

    @InjectMockKs
    lateinit var service: EthSignatureService

    @Test
    fun `validate - ok`() = runBlocking<Unit> {
        val expected = EthereumSignatureValidationFormDto(
            signer = randomAddress(),
            signature = randomBinary(),
            message = randomString()
        )

        coEvery { signatureControllerApi.validate(expected) } returns true.toMono()

        val result = service.validate(
            signer = EthConverter.convert(expected.signer),
            publicKey = null,
            signature = EthConverter.convert(expected.signature),
            message = expected.message
        )

        assertThat(result).isEqualTo(true)
    }

    @Test
    fun `get input - ok, opensea order fill input`() = runBlocking<Unit> {
        val input = randomBinary()
        val response = SeaportFulfillmentSimpleResponseDto(input)
        val orderId = randomString()

        every { signatureControllerApi.getSeaportOrderSignature(orderId) } returns response.toMono()

        val result = service.getInput(OpenSeaFillOrderSignatureInputFormDto(BlockchainDto.ETHEREUM, orderId))

        assertThat(result).isEqualTo(input.prefixed())
    }

    @Test
    fun `get input - ok, x2y2 order fill input`() = runBlocking<Unit> {
        val input = randomString()
        val response = X2Y2SignResponseDto(input)
        val request = X2Y2OrderSignRequestDto(
            caller = randomString(),
            op = randomBigInt(),
            orderId = randomBigInt(),
            currency = randomAddress(),
            price = randomBigInt(),
            tokenId = randomBigInt()
        )

        val form = X2Y2FillOrderSignatureInputFormDto(
            blockchain = BlockchainDto.POLYGON,
            caller = request.caller,
            op = request.op,
            orderId = request.orderId,
            currency = request.currency.prefixed(),
            price = request.price,
            tokenId = request.tokenId
        )

        every { signatureControllerApi.orderSignX2Y2(request) } returns response.toMono()

        val result = service.getInput(form)

        assertThat(result).isEqualTo(input)
    }

    @Test
    fun `get input - ok, x2y2 order cancel input`() = runBlocking<Unit> {
        val input = randomString()
        val response = X2Y2SignResponseDto(input)
        val request = X2Y2GetCancelInputRequestDto(
            caller = randomString(),
            op = randomBigInt(),
            orderId = randomBigInt(),
            signMessage = randomString(),
            sign = randomString(),
        )

        val form = X2Y2CancelOrderSignatureInputFormDto(
            blockchain = BlockchainDto.POLYGON,
            caller = request.caller,
            op = request.op,
            orderId = request.orderId,
            signMessage = request.signMessage,
            sign = request.sign,
        )

        every { signatureControllerApi.cancelSignX2Y2(request) } returns response.toMono()

        val result = service.getInput(form)

        assertThat(result).isEqualTo(input)
    }

    @Test
    fun `get input - failed, unsupported blockchain`() = runBlocking<Unit> {
        val orderId = randomString()

        assertThrows<UnionException> {
            service.getInput(OpenSeaFillOrderSignatureInputFormDto(BlockchainDto.MANTLE, orderId))
        }
    }
}
