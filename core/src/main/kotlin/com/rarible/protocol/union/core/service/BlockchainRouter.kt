package com.rarible.protocol.union.core.service

import com.rarible.protocol.union.dto.BlockchainDto
import kotlinx.coroutines.coroutineScope

abstract class BlockchainRouter<T : BlockchainService>(
    private val services: List<T>
) {

    private val blockchainServices = services.associateBy { it.getBlockchain() }
    private val supportedBlockchains = BlockchainDto.values().map { it.name }.toSet()

    fun getService(blockchain: String): T {
        if (!supportedBlockchains.contains(blockchain)) {
            throw IllegalArgumentException("Unsupported blockchain '$blockchain', supported are: $supportedBlockchains")
        }
        return blockchainServices[blockchain] ?: throw IllegalArgumentException(
            "Operation is not supported for '$blockchain', next blockchains available for it: ${blockchainServices.keys}"
        )
    }

    suspend fun <R> executeForAll(
        blockchains: Collection<BlockchainDto>?,
        clientCall: suspend (service: T) -> R
    ) = coroutineScope {
        val selectedServices = blockchains?.map { getService(it.name) } ?: services
        val combined = ArrayList<R>(selectedServices.size)
        for (service in selectedServices) {
            val singleResult = safeApiCall { clientCall(service) }
            if (singleResult != null) {
                combined.add(singleResult)
            }
        }
        combined
    }

    private suspend fun <T> safeApiCall(
        clientCall: suspend () -> T
    ): T? {
        try {
            return clientCall()
        } catch (e: Exception) {
            // TODO improve logging
            return null
        }
    }
}