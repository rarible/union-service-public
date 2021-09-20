package com.rarible.protocol.union.core.ethereum.service

import com.rarible.protocol.dto.EthereumSignatureValidationFormDto
import com.rarible.protocol.order.api.client.OrderSignatureControllerApi
import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.dto.EthBlockchainDto
import com.rarible.protocol.union.dto.UnionSignatureValidationFormDto
import io.daonomic.rpc.domain.Binary
import kotlinx.coroutines.reactive.awaitFirst
import scalether.domain.Address

class EthereumSignatureService(
    blockchain: EthBlockchainDto,
    private val signatureControllerApi: OrderSignatureControllerApi
) : AbstractEthereumService(blockchain), SignatureService {

    override suspend fun validate(form: UnionSignatureValidationFormDto): Boolean {
        val ethereumForm = EthereumSignatureValidationFormDto(
            signer = Address.apply(form.signer),
            message = form.message,
            signature = Binary.apply(form.signature)
        )
        return signatureControllerApi.validate(ethereumForm).awaitFirst()
    }
}