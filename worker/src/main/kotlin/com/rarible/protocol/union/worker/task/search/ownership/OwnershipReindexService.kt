package com.rarible.protocol.union.worker.task.search.ownership

import com.rarible.protocol.union.api.client.OwnershipControllerApi
import com.rarible.protocol.union.core.converter.EsOwnershipConverter
import com.rarible.protocol.union.dto.BlockchainDto
import com.rarible.protocol.union.enrichment.repository.search.EsOwnershipRepository
import com.rarible.protocol.union.worker.metrics.SearchTaskMetricFactory
import kotlinx.coroutines.flow.Flow

class OwnershipReindexService(
    private val ownershipClient: OwnershipControllerApi,
    private val esOwnershipRepository: EsOwnershipRepository,
    private val searchTaskMetricFactory: SearchTaskMetricFactory,
    private val converter: EsOwnershipConverter,
) {

    fun reindex(
        blockchain: BlockchainDto,
        target: OwnershipTaskParam.Target,
        index: String?,
        cursor: String? = null,
    ): Flow<String> = when (target) {
        OwnershipTaskParam.Target.OWNERSHIP -> TODO()
        OwnershipTaskParam.Target.AUCTIONED_OWNERSHIP -> TODO()
    }
}
