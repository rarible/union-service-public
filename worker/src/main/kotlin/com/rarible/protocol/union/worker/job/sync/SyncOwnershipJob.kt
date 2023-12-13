package com.rarible.protocol.union.worker.job.sync

import com.rarible.core.common.mapAsync
import com.rarible.protocol.union.core.converter.EsOwnershipConverter
import com.rarible.protocol.union.core.model.UnionAuctionOwnershipWrapper
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.producer.UnionInternalOwnershipEventProducer
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.core.task.AbstractSyncJobParam
import com.rarible.protocol.union.core.task.SyncScope
import com.rarible.protocol.union.core.util.toSlice
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.continuation.page.Slice
import com.rarible.protocol.union.enrichment.model.ShortOwnership
import com.rarible.protocol.union.enrichment.model.ShortOwnershipId
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.enrichment.service.EnrichmentOwnershipService
import com.rarible.protocol.union.worker.task.search.EsRateLimiter
import org.elasticsearch.action.support.WriteRequest
import org.springframework.stereotype.Component
import java.math.BigInteger

@Component
class SyncOwnershipJob(
    private val ownershipServiceRouter: BlockchainRouter<OwnershipService>,
    private val enrichmentOwnershipService: EnrichmentOwnershipService,
    private val esOwnershipRepository: EsOwnershipRepository,
    private val producer: UnionInternalOwnershipEventProducer,
    esRateLimiter: EsRateLimiter
) : AbstractSyncJob<UnionOwnership, ShortOwnership, SyncOwnershipJobParam>(
    "Ownership",
    SyncOwnershipJobParam::class.java,
    esRateLimiter
) {

    override suspend fun getNext(param: SyncOwnershipJobParam, state: String?): Slice<UnionOwnership> {
        val service = ownershipServiceRouter.getService(param.blockchain)
        return when {
            param.owner != null -> service.getOwnershipsByOwner(param.owner, state, param.batchSize).toSlice()
            else -> service.getOwnershipsAll(state, param.batchSize)
        }
    }

    override suspend fun updateDb(
        param: SyncOwnershipJobParam,
        unionEntities: List<UnionOwnership>
    ): List<ShortOwnership> {
        // TODO nothing to update, implement when we start to store Ownerships in Union
        return unionEntities.chunked(param.chunkSize).map { chunk ->
            chunk.mapAsync {
                enrichmentOwnershipService.getOrEmpty(ShortOwnershipId(it.id))
            }
        }.flatten()
    }

    override suspend fun updateEs(
        param: SyncOwnershipJobParam,
        enrichmentEntities: List<ShortOwnership>,
        unionEntities: List<UnionOwnership>
    ) {
        // TODO ideally we should have ES service to perform such bulk updates,
        // duplicated logic is here: OwnershipEventHandler
        val deleted = ArrayList<String>()
        val exist = unionEntities.filter {
            if (it.value == BigInteger.ZERO && it.lazyValue == BigInteger.ZERO) {
                deleted.add(it.id.fullId())
                false
            } else {
                true
            }
        }
        // TODO get rid of auctions and wrappers
        // TODO we can use here ShortOwnerships
        val dto = enrichmentOwnershipService.enrich(exist.map { UnionAuctionOwnershipWrapper(it, null) })
        val esOwnerships = dto.map { EsOwnershipConverter.convert(it) }
        esOwnershipRepository.bulk(esOwnerships, deleted, param.esIndex, WriteRequest.RefreshPolicy.NONE)
    }

    override suspend fun notify(
        param: SyncOwnershipJobParam,
        enrichmentEntities: List<ShortOwnership>,
        unionEntities: List<UnionOwnership>
    ) {
        producer.sendChangeEvents(enrichmentEntities.map { it.id.toDto() })
    }
}

data class SyncOwnershipJobParam(
    override val blockchain: BlockchainDto,
    override val scope: SyncScope,
    override val esIndex: String? = null,
    override val batchSize: Int = DEFAULT_BATCH,
    override val chunkSize: Int = DEFAULT_CHUNK,
    val owner: String? = null
) : AbstractSyncJobParam()
