package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.protocol.union.core.exception.UnionValidationException
import com.rarible.tzkt.client.SignatureClient
import com.rarible.tzkt.client.SignatureValidationException

class TzktSignatureServiceImpl(
    val signatureClient: SignatureClient
) : TzktSignatureService {

    override fun enabled() = true

    override suspend fun validate(publicKey: String, signature: String, message: String): Boolean {
        try {
            return signatureClient.validate(publicKey, signature, message)
        } catch (ex: SignatureValidationException) {
            throw UnionValidationException(ex.message)
        }
    }

}
