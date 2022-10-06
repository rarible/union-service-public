package com.rarible.protocol.union.worker.task.search.ownership

import com.rarible.protocol.union.core.converter.EsOwnershipConverter
import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.core.model.UnionAuctionOwnershipWrapper
import com.rarible.protocol.union.core.task.OwnershipTaskParam
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.PageSize
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.worker.metrics.SearchTaskMetricFactory
import com.rarible.protocol.union.worker.task.search.RateLimiter
import com.rarible.protocol.union.worker.task.search.activity.TimePeriodContinuationHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.elasticsearch.action.support.WriteRequest
import org.springframework.stereotype.Service

@Service
class OwnershipReindexService(
    private val repository: EsOwnershipRepository,
    private val searchTaskMetricFactory: SearchTaskMetricFactory,
    private val rawOwnershipClient: RawOwnershipClient,
    private val enrichmentOwnershipService: EnrichmentOwnershipService,
    private val rateLimiter: RateLimiter,
) {

    fun reindex(
        blockchain: BlockchainDto,
        target: OwnershipTaskParam.Target,
        index: String?,
        cursor: String? = null,
        from: Long? = null,
        to: Long? = null,
    ): Flow<String> = when (target) {
        OwnershipTaskParam.Target.OWNERSHIP -> reindexOwnerships(blockchain, target, index, cursor, from, to)
        OwnershipTaskParam.Target.AUCTIONED_OWNERSHIP ->
            reindexAuctionedOwnership(blockchain, target, index, cursor, from, to)
    }

    private fun reindexAuctionedOwnership(
        blockchain: BlockchainDto,
        target: OwnershipTaskParam.Target,
        index: String?,
        cursor: String?,
        from: Long? = null,
        to: Long? = null,
    ): Flow<String> = doReindex(blockchain, target, index, cursor, EsOwnershipConverter::convert, from, to) {
        val size = PageSize.OWNERSHIP.max
        rateLimiter.waitIfNecessary(size)
        // TODO enrich auctioned ownerships with bestSellOrder when it will be live
        rawOwnershipClient.getAuctionAll(blockchain, it, size)
    }

    private fun reindexOwnerships(
        blockchain: BlockchainDto,
        target: OwnershipTaskParam.Target,
        index: String?,
        cursor: String?,
        from: Long? = null,
        to: Long? = null,
    ): Flow<String> = doReindex(blockchain, target, index, cursor, EsOwnershipConverter::convert, from, to) {
        // TODO read values from config
        val size = when (blockchain) {
            BlockchainDto.SOLANA -> 250
            BlockchainDto.IMMUTABLEX -> 200 // Max size allowed by IMX
            else -> PageSize.OWNERSHIP.max
        }
        rateLimiter.waitIfNecessary(size)
        val unionOwnerships = rawOwnershipClient.getRawOwnershipsAll(blockchain, it, size)
        val ownershipsDtos = enrichmentOwnershipService.enrich(unionOwnerships.entities.map { unionOwnership ->
            UnionAuctionOwnershipWrapper(unionOwnership, null) }
        )
        Slice(unionOwnerships.continuation, ownershipsDtos)
    }

    private fun <T> doReindex(
        blockchain: BlockchainDto,
        target: OwnershipTaskParam.Target,
        index: String?,
        cursor: String?,
        convert: (T) -> EsOwnership,
        from: Long? = null,
        to: Long? = null,
        job: suspend (String?) -> Slice<T>,
    ) = flow {
        val counter = searchTaskMetricFactory.createReindexOwnershipCounter(blockchain, target)
        var current: String? = cursor
        do {
            val result = job(current)
            current = TimePeriodContinuationHelper.adjustContinuation(result.continuation, from, to)

            val ownerships = result.entities.map { convert(it) }
            val saved = repository.saveAll(ownerships, index, WriteRequest.RefreshPolicy.NONE)
            counter.increment(saved.size)

            emit(current.orEmpty())
        } while (current.isNullOrEmpty().not())
    }
}
