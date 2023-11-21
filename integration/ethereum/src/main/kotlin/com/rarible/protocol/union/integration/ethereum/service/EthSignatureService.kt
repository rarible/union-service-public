package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.protocol.dto.EthereumSignatureValidationFormDto
import com.rarible.protocol.dto.X2Y2GetCancelInputRequestDto
import com.rarible.protocol.dto.X2Y2OrderSignRequestDto
import com.rarible.protocol.order.api.client.OrderSignatureControllerApi
import com.rarible.protocol.union.core.exception.UnionException
import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.OpenSeaFillOrderSignatureInputFormDto
import com.rarible.protocol.union.dto.SignatureInputFormDto
import com.rarible.protocol.union.dto.X2Y2CancelOrderSignatureInputFormDto
import com.rarible.protocol.union.dto.X2Y2FillOrderSignatureInputFormDto
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import kotlinx.coroutines.reactive.awaitFirst
import kotlinx.coroutines.reactive.awaitSingle
import scalether.domain.Address

class EthSignatureService(
    blockchain: BlockchainDto,
    private val signatureControllerApi: OrderSignatureControllerApi
) : AbstractBlockchainService(blockchain), SignatureService {

    override suspend fun validate(
        signer: String,
        publicKey: String?,
        signature: String,
        message: String,
        algorithm: String?,
    ): Boolean {
        val ethereumForm = EthereumSignatureValidationFormDto(
            signer = EthConverter.convertToAddress(signer),
            signature = EthConverter.convertToBinary(signature),
            message = message
        )
        return signatureControllerApi.validate(ethereumForm).awaitFirst()
    }

    override suspend fun getInput(form: SignatureInputFormDto): String {
        // Aggregations supported only for OS/ETH
        if (form.blockchain != BlockchainDto.POLYGON && form.blockchain != BlockchainDto.ETHEREUM) {
            throw UnionException("Operation is not supported for ${blockchain.name}")
        }

        return when (form) {
            is OpenSeaFillOrderSignatureInputFormDto -> {
                signatureControllerApi.getSeaportOrderSignature(form.signature)
                    .awaitSingle().signature.prefixed()
            }

            is X2Y2FillOrderSignatureInputFormDto -> {
                signatureControllerApi.orderSignX2Y2(
                    X2Y2OrderSignRequestDto(
                        caller = form.caller,
                        op = form.op,
                        orderId = form.orderId,
                        currency = Address.apply(form.currency),
                        price = form.price,
                        tokenId = form.tokenId
                    )
                ).awaitSingle().input
            }

            is X2Y2CancelOrderSignatureInputFormDto -> {
                signatureControllerApi.cancelSignX2Y2(
                    X2Y2GetCancelInputRequestDto(
                        caller = form.caller,
                        op = form.op,
                        orderId = form.orderId,
                        signMessage = form.signMessage,
                        sign = form.sign
                    )
                ).awaitSingle().input
            }
        }
    }
}
