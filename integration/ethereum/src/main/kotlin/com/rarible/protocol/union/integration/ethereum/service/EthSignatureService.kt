package com.rarible.protocol.union.integration.ethereum.service

import com.rarible.protocol.dto.EthereumSignatureValidationFormDto
import com.rarible.protocol.order.api.client.OrderSignatureControllerApi
import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.reactive.awaitFirst
import scalether.domain.Address

class EthSignatureService(
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
            signer = Address.apply(signer),
            signature = Binary.apply(signature),
            message = message
        )
        return signatureControllerApi.validate(ethereumForm).awaitFirst()
    }
}