package com.rarible.protocol.union.core

import com.rarible.core.application.ApplicationEnvironmentInfo
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates
import org.springframework.stereotype.Component

@Component
class EsIndexProvider(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo,
) {

    private val env = applicationEnvironmentInfo.name

    fun getReadIndexCoords(entity: String): IndexCoordinates {
        return IndexCoordinates.of(getReadIndexName(entity))
    }

    fun getWriteIndexCoords(entity: String): IndexCoordinates {
        return IndexCoordinates.of(getReadIndexName(entity), getWriteIndexName(entity))
    }

    private fun getReadIndexName(entity: String): String {
        return "protocol_union_${env}_${entity}"
    }

    private fun getWriteIndexName(entity: String): String {
        return "protocol_union_${env}_${entity}_write"
    }
}
