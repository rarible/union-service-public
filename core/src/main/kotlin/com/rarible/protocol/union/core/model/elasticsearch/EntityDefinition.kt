package com.rarible.protocol.union.core.model.elasticsearch

data class EntityDefinition(
    val name: String,
    val mapping: String,
    val versionData: Int,
    val settings: String,
    val reindexTaskName: String
)
