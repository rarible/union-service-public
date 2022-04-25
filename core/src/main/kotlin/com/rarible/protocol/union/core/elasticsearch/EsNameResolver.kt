package com.rarible.protocol.union.core.elasticsearch

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinition
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinitionExtended
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.stereotype.Component

@Component
class EsNameResolver(
    val applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {
    final val metadataIndexName = "${PREFIX}_${applicationEnvironmentInfo.name}_$METADATA_INDEX"

    val matadataIndexCoordinate = IndexCoordinates.of(metadataIndexName)

    fun createEntityDefinitionExtended(entity: EntityDefinition) =
        EntityDefinitionExtended(
            name = entity.name,
            mapping = entity.mapping,
            versionData = entity.versionData,
            indexRootName = indexRootName(entity),
            aliasName = aliasName(entity),
            writeAliasName = writeAliasName(entity),
            settings = entity.settings,
            reindexTaskName = entity.reindexTaskName
        )

    private fun indexRootName(entity: EntityDefinition) =
        "${PREFIX}_${applicationEnvironmentInfo.name}_${entity.name}_"

    private fun aliasName(entity: EntityDefinition) =
        "${PREFIX}_${applicationEnvironmentInfo.name}_${entity.name}_alias"

    private fun writeAliasName(entity: EntityDefinition) =
        "${PREFIX}_${applicationEnvironmentInfo.name}_${entity.name}_write_alias"

    companion object {
        const val METADATA_INDEX = "metadata"
        private const val PREFIX = "protocol_union"
    }
}