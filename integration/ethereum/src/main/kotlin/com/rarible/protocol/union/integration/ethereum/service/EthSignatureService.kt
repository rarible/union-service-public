package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.dto.EthereumSignatureValidationFormDto
import com.rarible.protocol.order.api.client.OrderSignatureControllerApi
import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.ethereum.converter.EthConverter
import kotlinx.coroutines.reactive.awaitFirst

open class EthSignatureService(
    blockchain: BlockchainDto,
    private val signatureControllerApi: OrderSignatureControllerApi
) : AbstractBlockchainService(blockchain), SignatureService {

    override suspend fun validate(
        signer: String,
        publicKey: String?,
        signature: String,
        message: String
    ): Boolean {
        val ethereumForm = EthereumSignatureValidationFormDto(
            signer = EthConverter.convertToAddress(signer),
            signature = EthConverter.convertToBinary(signature),
            message = message
        )
        return signatureControllerApi.validate(ethereumForm).awaitFirst()
    }
}

@CaptureSpan(type = "network", subtype = "ethereum")
open class EthereumSignatureService(
    signatureControllerApi: OrderSignatureControllerApi
) : EthSignatureService(
    BlockchainDto.ETHEREUM,
    signatureControllerApi
)

@CaptureSpan(type = "network", subtype = "polygon")
open class PolygonSignatureService(
    signatureControllerApi: OrderSignatureControllerApi
) : EthSignatureService(
    BlockchainDto.POLYGON,
    signatureControllerApi
)