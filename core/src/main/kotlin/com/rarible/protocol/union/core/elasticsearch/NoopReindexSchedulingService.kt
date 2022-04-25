package com.rarible.protocol.union.core.elasticsearch

import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinitionExtended

class NoopReindexSchedulingService(
    private val indexMetadataService: IndexMetadataService
) : ReindexSchedulingService {
    override fun scheduleReindex(
        newIndexName: String,
        entityDefinition: EntityDefinitionExtended,
        indexSettings: String
    ) {
        indexMetadataService.updateMetadata(
            entityDefinition = entityDefinition,
            settings = indexSettings
        )
    }
}
