package com.rarible.protocol.union.core.model.elastic

import com.rarible.protocol.union.core.model.elastic.SettingsHasher.md5

data class EntityDefinition(
    val entity: EsEntity,
    val mapping: String,
    val versionData: Int,
    val settings: String
) {
    val reindexTask = "${entity.name}_REINDEX"
    val settingsHash = md5(settings)
}
