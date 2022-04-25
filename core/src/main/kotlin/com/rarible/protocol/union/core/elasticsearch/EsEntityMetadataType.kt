package com.rarible.protocol.union.core.elasticsearch

import com.rarible.protocol.union.core.model.elasticsearch.EntityDefinitionExtended

enum class EsEntityMetadataType(val suffix: String) {
    MAPPING("_mapping"),
    SETTINGS("_settings"),
    VERSION_DATA("_version"),
}

fun EsEntityMetadataType.getId(entityDefinition: EntityDefinitionExtended) = entityDefinition.name + suffix
