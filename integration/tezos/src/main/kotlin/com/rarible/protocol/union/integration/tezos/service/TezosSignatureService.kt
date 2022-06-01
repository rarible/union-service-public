package com.rarible.protocol.union.integration.tezos.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.tezos.api.client.OrderSignatureControllerApi
import com.rarible.protocol.tezos.dto.SignatureValidationFormDto
import com.rarible.protocol.union.core.exception.UnionValidationException
import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktSignatureService
import kotlinx.coroutines.reactive.awaitFirst

@CaptureSpan(type = "blockchain")
open class TezosSignatureService(
    private val signatureControllerApi: OrderSignatureControllerApi,
    private val tzktSignatureService: TzktSignatureService
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

        if (tzktSignatureService.enabled()) {
            return tzktSignatureService.validate(publicKey, signature, message)
        } else {
            val pair = publicKey.split('_')
            val edpk = pair[0]
            // Do not trim prefix, Tezos is sensitive for leading/trailing spaces in prefix
            val prefix = pair.getOrNull(1)

            val tezosForm = SignatureValidationFormDto(
                address = signer,
                edpk = edpk,
                signature = signature,
                message = message,
                prefix = prefix
            )
            return signatureControllerApi.validate(tezosForm).awaitFirst()
        }
    }
}
