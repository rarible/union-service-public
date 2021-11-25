package com.rarible.protocol.union.integration.tezos.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.tezos.api.client.OrderSignatureControllerApi
import com.rarible.protocol.tezos.dto.SignatureValidationFormDto
import com.rarible.protocol.union.core.exception.UnionValidationException
import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import kotlinx.coroutines.reactive.awaitFirst

@CaptureSpan(type = "blockchain")
open class TezosSignatureService(
    private val signatureControllerApi: OrderSignatureControllerApi
) : AbstractBlockchainService(BlockchainDto.TEZOS), SignatureService {

    override suspend fun validate(
        signer: String,
        publicKey: String?,
        signature: String,
        message: String
    ): Boolean {
        if (publicKey.isNullOrBlank()) {
            throw UnionValidationException("Public key is not specified")
        }
        val tezosForm = SignatureValidationFormDto(
            address = signer,
            edpk = publicKey,
            signature = signature,
            message = message
        )
        return signatureControllerApi.validate(tezosForm).awaitFirst()
    }
}
