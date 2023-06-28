package com.rarible.protocol.union.core.kafka

import org.springframework.stereotype.Component

@Component
class KafkaGroupFactory {

    fun metaDownloadExecutorGroup(type: String): String {
        return "protocol.union.download.executor.meta.$type"
    }

    companion object {
        const val COLLECTION_TYPE = "collection"
        const val ITEM_TYPE = "item"
    }
}