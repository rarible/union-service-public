package com.rarible.protocol.union.integration.flow.service

import com.rarible.protocol.flow.nft.api.client.FlowNftCryptoControllerApi
import com.rarible.protocol.union.core.exception.UnionValidationException
import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import kotlinx.coroutines.reactive.awaitFirst
import org.apache.commons.lang3.StringUtils

class FlowSignatureService(
    private val signatureControllerApi: FlowNftCryptoControllerApi
) : AbstractBlockchainService(BlockchainDto.FLOW), SignatureService {

    override suspend fun validate(
        signer: String,
        publicKey: String?,
        signature: String,
        message: String
    ): Boolean {
        if (StringUtils.isBlank(publicKey)) {
            throw UnionValidationException("Parameter publicKey is not specified: $publicKey")
        }
        return signatureControllerApi.verifySignature(
            publicKey,
            signer,
            signature,
            message
        ).awaitFirst()
    }
}