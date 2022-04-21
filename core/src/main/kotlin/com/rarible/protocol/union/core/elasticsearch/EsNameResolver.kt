package com.rarible.protocol.union.core.elasticsearch

import com.rarible.core.application.ApplicationEnvironmentInfo
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinition
import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinitionExtended
import org.springframework.stereotype.Component

@Component
class EsNameResolver(
    val applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {

    fun metadataIndexName() = "${PREFIX}_${applicationEnvironmentInfo.name}_$METADATA_INDEX"

    fun createEntityDefinitionExtended(entity: EntityDefinition) =
        EntityDefinitionExtended(
            entity.name, entity.mapping, entity.versionData,
            indexRootName = indexRootName(entity),
            aliasName = aliasName(entity),
            writeAliasName = writeAliasName(entity),
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