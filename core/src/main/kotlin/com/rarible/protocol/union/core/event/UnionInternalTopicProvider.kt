package com.rarible.protocol.union.core.event

import com.rarible.protocol.union.dto.BlockchainDto

object UnionInternalTopicProvider {

    const val VERSION = "v1"

    fun getInternalBlockchainTopic(environment: String, blockchain: BlockchainDto): String {
        return "protocol.$environment.union.internal.blockchain.${blockchain.name.lowercase()}"
    }

    fun getReconciliationMarkTopic(environment: String): String {
        return "protocol.$environment.union.internal.reconciliation"
    }

    fun getItemMetaDownloadTaskSchedulerTopic(environment: String): String {
        return "protocol.$environment.union.internal.download.meta.item"
    }

    fun getItemMetaDownloadTaskExecutorTopic(environment: String, pipeline: String): String {
        return "protocol.$environment.union.internal.download.meta.item.$pipeline"
    }

    fun getCollectionMetaDownloadTaskSchedulerTopic(environment: String): String {
        return "protocol.$environment.union.internal.download.meta.collection"
    }

    fun getCollectionMetaDownloadTaskExecutorTopic(environment: String, pipeline: String): String {
        return "protocol.$environment.union.internal.download.meta.collection.$pipeline"
    }

    fun getItemChangeTopic(environment: String): String {
        return "protocol.$environment.union.internal.change.item"
    }

    fun getTraitTopic(environment: String): String {
        return "protocol.$environment.union.internal.trait"
    }
}
