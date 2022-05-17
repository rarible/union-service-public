package com.rarible.protocol.union.core.model.elasticsearch

data class EntityDefinition(
    val entity: EsEntity,
    val mapping: String,
    val versionData: Int,
    val settings: String
) {
    val reindexTask = "${entity.name}_REINDEX"
}