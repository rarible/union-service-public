package com.rarible.protocol.union.integration.flow.service

import com.rarible.protocol.flow.nft.api.client.FlowNftCryptoControllerApi
import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import kotlinx.coroutines.reactive.awaitFirst

class FlowSignatureService(
    private val signatureControllerApi: FlowNftCryptoControllerApi
) : AbstractBlockchainService(BlockchainDto.FLOW), SignatureService {

    override suspend fun validate(
        signer: String,
        signature: String,
        message: String
    ): Boolean {
        return signatureControllerApi.verifySignature(
            signer,
            signature,
            message
        ).awaitFirst()
    }
}