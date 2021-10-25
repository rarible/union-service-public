package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.core.service.router.BlockchainService

interface SignatureService : BlockchainService {

    suspend fun validate(
        signer: String,
        publicKey: String?,
        signature: String,
        message: String
    ): Boolean

}