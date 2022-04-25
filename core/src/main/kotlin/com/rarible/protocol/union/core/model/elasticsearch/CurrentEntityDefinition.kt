package com.rarible.protocol.union.core.model.elasticsearch

data class CurrentEntityDefinition(
    val mapping: String,
    val versionData: Int,
    val settings: String,
)
