package com.rarible.protocol.union.core.service.dummy

import com.rarible.protocol.union.core.exception.UnionException
import com.rarible.protocol.union.core.service.SignatureService
import com.rarible.protocol.union.core.service.router.AbstractBlockchainService
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.SignatureInputFormDto

class DummySignatureService(
    blockchain: BlockchainDto
) : AbstractBlockchainService(blockchain), SignatureService {

    override suspend fun validate(
        signer: String,
        publicKey: String?,
        signature: String,
        message: String,
        algorithm: String?,
    ): Boolean {
        return false
    }

    override suspend fun getInput(form: SignatureInputFormDto): String {
        throw UnionException("Operation is not supported for ${blockchain.name}")
    }
}
