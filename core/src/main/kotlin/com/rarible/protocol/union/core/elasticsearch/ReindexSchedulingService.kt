package com.rarible.protocol.union.core.elasticsearch

import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinitionExtended

interface ReindexSchedulingService {
    fun scheduleReindex(
        newIndexName: String,
        entityDefinition: EntityDefinitionExtended,
        indexSettings: String
    )
}
