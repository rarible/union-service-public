package com.rarible.protocol.union.integration.tezos.service

import com.rarible.core.apm.CaptureSpan
import com.rarible.protocol.union.core.exception.UnionValidationException
import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktSignatureService

@CaptureSpan(type = "blockchain")
open class TezosSignatureService(
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

        return tzktSignatureService.validate(publicKey, signature, message)
    }
}
