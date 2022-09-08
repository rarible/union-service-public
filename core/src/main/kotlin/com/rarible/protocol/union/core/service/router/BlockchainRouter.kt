package com.rarible.protocol.union.core.service.router

import com.rarible.core.client.WebClientResponseProxyException
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger

class BlockchainRouter<T : BlockchainService>(
    services: List<T>,
    blockchains: List<BlockchainDto>
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    // Enabled blockchains, executeForAll should consider this set
    private val enabledBlockchains = blockchains.toSet()

    // All services, include dummy for disabled blockchains - for getService method
    private val blockchainServices = services.associateBy { it.blockchain }

    fun getService(blockchain: BlockchainDto): T {
        return blockchainServices[blockchain] ?: throw IllegalArgumentException(
            "Operation is not supported for '$blockchain', next blockchains available for it: ${blockchainServices.keys}"
        )
    }

    fun isBlockchainEnabled(blockchain: BlockchainDto): Boolean {
        return enabledBlockchains.contains(blockchain)
    }

    fun getEnabledBlockchains(blockchains: Collection<BlockchainDto>?) =
        if (blockchains == null || blockchains.isEmpty()) {
            enabledBlockchains
        } else {
            blockchains.filter(enabledBlockchains::contains)
        }

    suspend fun <R> fetchAllBySlices(
        blockchain: BlockchainDto,
        clientCall: suspend (service: T, continuation: String?) -> Slice<R>,
        consumer: suspend (slice: Slice<R>) -> Unit
    ) {
        val service = getService(blockchain)
        var continuation: String? = null
        do {
            val slice = clientCall(service, continuation)
            if (slice.entities.isEmpty()) {
                return
            }
            consumer(slice)
            if (slice.continuation.isNullOrBlank()) {
                return
            }
            continuation = slice.continuation
        } while (true)
    }

    suspend fun <R> fetchAllBySlices(
        blockchain: BlockchainDto,
        clientCall: suspend (service: T, continuation: String?) -> Slice<R>
    ): List<R> {
        val slices = ArrayList<List<R>>()
        val size = AtomicInteger(0)
        fetchAllBySlices(blockchain, clientCall) { slice ->
            slices.add(slice.entities)
            size.addAndGet(slice.entities.size)
        }
        val result = ArrayList<R>(size.get())
        slices.forEach { result.addAll(it) }
        return result
    }

    suspend fun <R : Any> executeForAll(
        blockchains: Collection<BlockchainDto>?,
        clientCall: suspend (service: T) -> R
    ): List<R> = coroutineScope {
        val enabledBlockchains = if (blockchains == null || blockchains.isEmpty()) {
            enabledBlockchains
        } else {
            blockchains.filter { enabledBlockchains.contains(it) }
        }
        val selectedServices = enabledBlockchains.map { getService(it) }

        if (selectedServices.size == 1) {
            // For single blockchain we are not using safe call in order to throw exception
            listOf(clientCall(selectedServices[0]))
        } else {
            selectedServices.map {
                async {
                    safeApiCall { clientCall(it) }
                }
            }.awaitAll().filterNotNull()
        }
    }

    private suspend fun <T> safeApiCall(
        clientCall: suspend () -> T
    ): T? {
        return try {
            clientCall()
        } catch (e: WebClientResponseProxyException) {
            logger.warn("Received an error from API Client: {} with message: {}", e.data, e.message)
            null
        } catch (e: Throwable) {
            logger.error("Unexpected error from API Client: {}", e.message, e)
            null
        }
    }
}
