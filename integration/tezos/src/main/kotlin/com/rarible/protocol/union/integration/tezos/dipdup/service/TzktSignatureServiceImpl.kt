package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.protocol.union.core.exception.UnionValidationException
import com.rarible.tzkt.client.SignatureClient
import com.rarible.tzkt.client.SignatureValidationException

class TzktSignatureServiceImpl(
    val signatureClient: SignatureClient
) : TzktSignatureService {

    override suspend fun validate(publicKey: String, signature: String, message: String): Boolean {
        try {
            val pair = publicKey.split('_')
            val edpk = pair[0]
            // Do not trim prefix, Tezos is sensitive for leading/trailing spaces in prefix
            val prefix = pair.getOrNull(1)
            val fullMsg = prefix?.let { it+message } ?: message

            return signatureClient.validate(edpk, signature, fullMsg)
        } catch (ex: SignatureValidationException) {
            throw UnionValidationException(ex.message)
        }
    }

}
