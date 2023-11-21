package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.core.service.router.BlockchainService
import com.rarible.protocol.union.dto.SignatureInputFormDto

interface SignatureService : BlockchainService {

    suspend fun validate(
        signer: String,
        publicKey: String?,
        signature: String,
        message: String,
        algorithm: String?
    ): Boolean

    // TODO Ideally there should be intermediate object SignatureInputForm with 'native' values only
    suspend fun getInput(form: SignatureInputFormDto): String
}
