package com.rarible.protocol.union.api.service

import com.rarible.protocol.union.core.continuation.CombinedContinuation
import com.rarible.protocol.union.core.continuation.page.ArgSlice
import com.rarible.protocol.union.core.continuation.page.Slice
import com.rarible.protocol.union.core.service.ActivityService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ActivityDto
import com.rarible.protocol.union.dto.ActivitySortDto
import com.rarible.protocol.union.dto.ActivityTypeDto
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.UserActivityTypeDto
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class ActivityApiService(
    private val router: BlockchainRouter<ActivityService>
) {

    suspend fun getAllActivities(
        type: List<ActivityTypeDto>,
        blockchains: List<BlockchainDto>?,
        cursor: String?,
        size: Int,
        sort: ActivitySortDto?
    ): List<ArgSlice<ActivityDto>> {
        val evaluatedBlockchains = router.getEnabledBlockchains(blockchains).map(BlockchainDto::name)
        val slices = getActivitiesByBlockchains(cursor, evaluatedBlockchains) { blockchain, continuation ->
            val blockDto = BlockchainDto.valueOf(blockchain)
            router.getService(blockDto).getAllActivities(type, continuation, size, sort)
        }
        return slices
    }

    suspend fun getActivitiesByUser(
        type: List<UserActivityTypeDto>,
        groupedByBlockchain: Map<BlockchainDto, List<String>>,
        from: Instant?,
        to: Instant?,
        cursor: String?,
        safeSize: Int,
        sort: ActivitySortDto?
    ): List<ArgSlice<ActivityDto>> {
        val evaluatedBlockchains = groupedByBlockchain.keys.map { it.name }
        val slices = getActivitiesByBlockchains(cursor, evaluatedBlockchains) { blockchain, continuation ->
            val blockDto = BlockchainDto.valueOf(blockchain)
            val users = groupedByBlockchain[blockDto] ?: listOf()
            router.getService(blockDto).getActivitiesByUser(type, users, from, to, continuation, safeSize, sort)
        }
        return slices
    }

    private suspend fun getActivitiesByBlockchains(
        continuation: String?,
        blockchains: Collection<String>,
        clientCall: suspend (blockchain: String, continuation: String?) -> Slice<ActivityDto>
    ): List<ArgSlice<ActivityDto>> {
        val currentContinuation = CombinedContinuation.parse(continuation)
        return coroutineScope {
            blockchains.map { blockchain ->
                async {
                    val blockchainContinuation = currentContinuation.continuations[blockchain]
                    // For completed blockchain we do not request orders
                    if (blockchainContinuation == ArgSlice.COMPLETED) {
                        ArgSlice(blockchain, blockchainContinuation, Slice(null, emptyList()))
                    } else {
                        ArgSlice(blockchain, blockchainContinuation, clientCall(blockchain, blockchainContinuation))
                    }
                }
            }
        }.awaitAll()
    }
}
