package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.common.flatMapAsync
import com.rarible.core.logging.Logger
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
import org.springframework.stereotype.Component

@Component
class OwnershipElasticHelper(
    private val repository: EsOwnershipRepository,
    private val router: BlockchainRouter<OwnershipService>,
) {

    suspend fun getRawOwnershipsByOwner(owner: UnionAddress, continuation: String?, size: Int): List<UnionOwnership> {
        val filter = EsOwnershipByOwnerFilter(owner, null, cursor = continuation)
        return repository.search(filter, size).getOwnerships()
    }

    suspend fun getRawOwnershipsByItem(itemId: ItemIdDto, continuation: String?, size: Int): List<UnionOwnership> {
        val filter = EsOwnershipByItemFilter(itemId, cursor = continuation)
        return repository.search(filter, size).getOwnerships()
    }

    private suspend fun List<EsOwnership>.getOwnerships(): List<UnionOwnership> {
        log.debug("Enrich elastic ownerships with blockchains api (count=$size)")
        val idsMap = groupBy { it.blockchain }
        val enabledBlockchains = router.getEnabledBlockchains(idsMap.keys).toSet()
        log.debug("Source blockchains: ${idsMap.keys.intersect(enabledBlockchains)} (${idsMap.keys - enabledBlockchains} disabled)")

        val ownerships = idsMap.filterKeys { it in enabledBlockchains }
            .flatMapAsync { (blockchain, ids) ->
                router.getService(blockchain).getOwnershipsByIds(ids.map { it.ownershipId })
            }

        log.debug("Obtained ${ownerships.size} ownerships")
        return ownerships
    }

    companion object {
        private val log by Logger()
    }
}
