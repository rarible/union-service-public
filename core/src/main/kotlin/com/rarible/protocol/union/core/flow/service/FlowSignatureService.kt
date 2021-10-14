package com.rarible.protocol.union.core.flow.service

import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.SignatureValidationFormDto

class FlowSignatureService(
    blockchain: BlockchainDto
    // private val signatureControllerApi: Any
) : AbstractBlockchainService(blockchain), SignatureService {

    override suspend fun validate(form: SignatureValidationFormDto): Boolean {
        return false // TODO implement later
    }
}