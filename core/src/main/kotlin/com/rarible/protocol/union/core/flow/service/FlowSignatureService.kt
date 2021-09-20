package com.rarible.protocol.union.core.flow.service

import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.dto.FlowBlockchainDto
import com.rarible.protocol.union.dto.UnionSignatureValidationFormDto

class FlowSignatureService(
    blockchain: FlowBlockchainDto
    // private val signatureControllerApi: Any
) : AbstractFlowService(blockchain), SignatureService {

    override suspend fun validate(form: UnionSignatureValidationFormDto): Boolean {
        return false // TODO implement later
    }
}