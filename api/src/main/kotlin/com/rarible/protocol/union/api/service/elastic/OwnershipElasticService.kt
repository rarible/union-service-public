package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.common.flatMapAsync
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.api.service.OwnershipQueryService
import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.core.model.EsOwnershipByItemFilter
import com.rarible.protocol.union.core.model.EsOwnershipByOwnerFilter
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.continuation.DateIdContinuation
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component

@Component
@Primary
@ConditionalOnProperty(
    value = ["enableOrderQueriesToElasticSearch"],
    prefix = "common.feature-flags",
)
class OwnershipElasticService(
    private val repository: EsOwnershipRepository,
    private val router: BlockchainRouter<OwnershipService>,
) : OwnershipQueryService {

    override suspend fun getOwnershipByOwner(
        owner: UnionAddress,
        continuation: String?,
        size: Int,
    ): List<UnionOwnership> = repository
        .findByFilter(EsOwnershipByOwnerFilter(owner, DateIdContinuation.parse(continuation), size))
        .getOwnerships()

    override suspend fun getOwnershipsByItem(
        itemId: ItemIdDto,
        continuation: String?,
        size: Int,
    ): List<UnionOwnership> = repository
        .findByFilter(EsOwnershipByItemFilter(itemId, DateIdContinuation.parse(continuation), size))
        .getOwnerships()

    suspend fun List<EsOwnership>.getOwnerships(): List<UnionOwnership> {
        log.debug("Enrich elastic ownerships with blockchains api (count=$size)")
        val idsMap = groupBy { it.blockchain }
        val enabledBlockchains = router.getEnabledBlockchains(idsMap.keys).toSet()
        log.debug("Source blockchains: ${idsMap.keys.intersect(enabledBlockchains)} (${idsMap.keys - enabledBlockchains} disabled)")

        val ownerships = idsMap.filterKeys { it in enabledBlockchains }
            .flatMapAsync { (blockchain, ids) ->
                router.getService(blockchain).getAllOwnerships(ids.map { it.ownershipId })
            }

        log.debug("Obtained ${ownerships.size} ownerships")
        return ownerships
    }

    companion object {
        private val log by Logger()
    }
}
