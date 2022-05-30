package com.rarible.protocol.union.integration.tezos.dipdup.service

interface TzktSignatureService {

    fun enabled() = false

    suspend fun validate(
        publicKey: String,
        signature: String,
        message: String
    ): Boolean = false

}
