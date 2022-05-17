package com.rarible.protocol.union.core.elasticsearch

import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinitionExtended

interface ReindexSchedulingService {
    suspend fun scheduleReindex(
        newIndexName: String,
        entityDefinition: EntityDefinitionExtended
    )
}
