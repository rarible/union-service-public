package com.rarible.protocol.union.core.elasticsearch

import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinitionExtended

class NoopReindexSchedulingService(
    private val indexService: IndexService
) : ReindexSchedulingService {

    override suspend fun scheduleReindex(
        newIndexName: String,
        entityDefinition: EntityDefinitionExtended
    ) {
        indexService.updateMetadata(
            entityDefinition = entityDefinition
        )
    }
}
