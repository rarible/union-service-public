package com.rarible.protocol.union.integration.tezos.service

import com.rarible.protocol.union.core.exception.UnionException
import com.rarible.protocol.union.core.exception.UnionValidationException
import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.SignatureInputFormDto
import com.rarible.protocol.union.integration.tezos.dipdup.service.TzktSignatureService

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

    override suspend fun getInput(form: SignatureInputFormDto): String {
        throw UnionException("Operation is not supported for ${blockchain.name}")
    }
}
