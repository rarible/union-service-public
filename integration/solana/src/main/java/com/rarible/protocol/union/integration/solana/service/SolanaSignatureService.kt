package com.rarible.protocol.union.integration.solana.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.solana.api.client.SignatureControllerApi
import com.rarible.protocol.solana.dto.SolanaSignatureValidationFormDto
import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import kotlinx.coroutines.reactive.awaitFirst

@CaptureSpan(type = "blockchain")
open class SolanaSignatureService(
    private val signatureControllerApi: SignatureControllerApi
) : AbstractBlockchainService(BlockchainDto.SOLANA), SignatureService {

    override suspend fun validate(
        signer: String,
        publicKey: String?,
        signature: String,
        message: String
    ): Boolean {
        return signatureControllerApi.validate(
            SolanaSignatureValidationFormDto(
                signer = signer,
                publicKey = publicKey,
                signature = signature,
                message = message,
            )
        ).awaitFirst()
    }
}
