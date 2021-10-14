package com.rarible.protocol.union.core.flow.service

import com.rarible.protocol.flow.nft.api.client.FlowNftCryptoControllerApi
import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.SignatureValidationFormDto
import kotlinx.coroutines.reactive.awaitFirst

class FlowSignatureService(
    blockchain: BlockchainDto,
    private val signatureControllerApi: FlowNftCryptoControllerApi
) : AbstractFlowService(blockchain), SignatureService {

    override suspend fun validate(form: SignatureValidationFormDto): Boolean {
        return signatureControllerApi.verifySignature(
            form.signer.value,
            form.signature,
            form.message
        ).awaitFirst()
    }
}