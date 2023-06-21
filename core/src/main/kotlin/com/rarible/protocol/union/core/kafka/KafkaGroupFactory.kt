package com.rarible.protocol.union.core.kafka

import com.rarible.core.application.ApplicationEnvironmentInfo
import org.springframework.stereotype.Component

@Component
class KafkaGroupFactory(
    applicationEnvironmentInfo: ApplicationEnvironmentInfo
) {
    private val env = applicationEnvironmentInfo.name

    fun metaDownloadExecutorGroup(type: String): String {
        return "$env.protocol.union.download.executor.meta.$type"
    }

    companion object {
        const val COLLECTION_TYPE = "collection"
        const val ITEM_TYPE = "item"
    }
}