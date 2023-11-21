package com.rarible.protocol.union.integration.flow.service

import com.rarible.protocol.flow.nft.api.client.FlowNftCryptoControllerApi
import com.rarible.protocol.union.core.exception.UnionException
import com.rarible.protocol.union.core.exception.UnionValidationException
import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.SignatureInputFormDto
import kotlinx.coroutines.reactive.awaitFirst
import org.apache.commons.lang3.StringUtils

open class FlowSignatureService(
    private val signatureControllerApi: FlowNftCryptoControllerApi
) : AbstractBlockchainService(BlockchainDto.FLOW), SignatureService {

    override suspend fun validate(
        signer: String,
        publicKey: String?,
        signature: String,
        message: String,
        algorithm: String?,
    ): Boolean {
        if (StringUtils.isBlank(publicKey)) {
            throw UnionValidationException("Parameter publicKey is not specified: $publicKey")
        }
        return signatureControllerApi.verifySignature(
            publicKey,
            signer,
            signature,
            message,
            algorithm,
        ).awaitFirst()
    }

    override suspend fun getInput(form: SignatureInputFormDto): String {
        throw UnionException("Operation is not supported for ${blockchain.name}")
    }
}
