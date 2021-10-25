package com.rarible.protocol.union.integration.tezos.service

import com.rarible.protocol.tezos.api.client.OrderSignatureControllerApi
import com.rarible.protocol.tezos.dto.SignatureValidationFormDto
import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import kotlinx.coroutines.reactive.awaitFirst

class TezosSignatureService(
    private val signatureControllerApi: OrderSignatureControllerApi
) : AbstractBlockchainService(BlockchainDto.TEZOS), SignatureService {

    override suspend fun validate(
        signer: String,
        signature: String,
        message: String
    ): Boolean {
        val tezosForm = SignatureValidationFormDto(
            signer = signer,
            signature = signature,
            message = message
        )
        return signatureControllerApi.validate(tezosForm).awaitFirst()
    }
}
