package com.rarible.protocol.union.core.service.dummy

import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto

class DummySignatureService(
    blockchain: BlockchainDto
) : AbstractBlockchainService(blockchain), SignatureService {

    override suspend fun validate(
        signer: String,
        signature: String,
        message: String
    ): Boolean {
        return false
    }
}