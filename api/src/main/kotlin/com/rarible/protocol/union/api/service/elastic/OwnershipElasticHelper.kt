package com.rarible.protocol.union.api.service.elastic

import com.rarible.core.common.mapAsync
import com.rarible.core.logging.Logger
import com.rarible.protocol.union.core.model.EsActivityCursor.Companion.fromActivityLite
import com.rarible.protocol.union.core.model.EsOwnership
import com.rarible.protocol.union.core.model.EsOwnershipByItemFilter
import com.rarible.protocol.union.core.model.EsOwnershipByOwnerFilter
import com.rarible.protocol.union.core.model.EsOwnershipsSearchFilter
import com.rarible.protocol.union.core.model.UnionOwnership
import com.rarible.protocol.union.core.service.OwnershipService
import com.rarible.protocol.union.core.service.router.BlockchainRouter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.dto.ItemIdDto
import com.rarible.protocol.union.dto.OwnershipSearchRequestDto
import com.rarible.protocol.union.dto.UnionAddress
import com.rarible.protocol.union.dto.parser.OwnershipIdParser
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import org.springframework.stereotype.Component

@Component
class OwnershipElasticHelper(
    private val repository: EsOwnershipRepository,
    private val router: BlockchainRouter<OwnershipService>,
) {

    suspend fun getRawOwnershipsByOwner(owner: UnionAddress, continuation: String?, size: Int): List<UnionOwnership> {
        val filter = EsOwnershipByOwnerFilter(owner, null, cursor = continuation)
        val ownerships = repository.search(filter, size)
        return getOwnerships(ownerships)
    }

    suspend fun getRawOwnershipsByItem(itemId: ItemIdDto, continuation: String?, size: Int): List<UnionOwnership> {
        val filter = EsOwnershipByItemFilter(itemId, cursor = continuation)
        val ownerships = repository.search(filter, size)
        return getOwnerships(ownerships)
    }

    suspend fun getRawOwnershipsBySearchRequest(request: OwnershipSearchRequestDto): List<UnionOwnership> {
        val filter = EsOwnershipsSearchFilter(request)
        return repository.search(filter, request.size).getOwnerships()
    }

    private suspend fun getOwnerships(esOwnerhips: List<EsOwnership>): List<UnionOwnership> {
        if (esOwnerhips.isEmpty()) return emptyList()
        log.debug("Enrich elastic ownerships with blockchains api (count=${esOwnerhips.size})")
        val mapping = hashMapOf<BlockchainDto, MutableList<String>>()
        esOwnerhips.forEach { ownership ->
            mapping
                .computeIfAbsent(ownership.blockchain) { ArrayList(esOwnerhips.size) }
                .add(OwnershipIdParser.parseFull(ownership.ownershipId).value)
        }

        val ownerships = mapping.mapAsync { (blockchain, ids) ->
            val isBlockchainEnabled = router.isBlockchainEnabled(blockchain)
            if (isBlockchainEnabled) router.getService(blockchain).getOwnershipsByIds(ids) else emptyList()
        }.flatten()

        val ownershipsIdMapping = ownerships.associateBy { it.id.fullId() }
        return esOwnerhips.mapNotNull { esOwnership -> ownershipsIdMapping[esOwnership.ownershipId] }
    }

    companion object {
        private val log by Logger()
    }
}
