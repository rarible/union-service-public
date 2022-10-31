package com.rarible.protocol.union.integration.tezos.dipdup.service

import com.rarible.dipdup.client.exception.DipDupNotFound
import com.rarible.protocol.union.core.exception.UnionNotFoundException

interface DipDupService {

    suspend fun <T> safeApiCall(msg: String?, clientCall: suspend () -> T): T {
        return try {
            clientCall()
        } catch (e: DipDupNotFound) {
            throw UnionNotFoundException(message = msg ?: e.message ?: "")
        }
    }

    suspend fun <T> safeApiCall(clientCall: suspend () -> T) = safeApiCall(null, clientCall)
}
