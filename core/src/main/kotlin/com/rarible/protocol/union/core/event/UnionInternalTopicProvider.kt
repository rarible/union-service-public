package com.rarible.protocol.union.core.event

import com.rarible.protocol.union.dto.BlockchainDto

object UnionInternalTopicProvider {

    const val VERSION = "v1"

    @Deprecated("Replaced by blockchain topics")
    fun getWrappedTopic(environment: String): String {
        return "protocol.$environment.union.internal.wrapped"
    }

    fun getInternalBlockchainTopic(environment: String, blockchain: BlockchainDto): String {
        return "protocol.$environment.union.internal.blockchain.${blockchain.name.lowercase()}"
    }

    fun getReconciliationMarkTopic(environment: String): String {
        return "protocol.$environment.union.internal.reconciliation"
    }

    fun getItemMetaDownloadTaskSchedulerTopic(environment: String): String {
        return "protocol.$environment.union.internal.download.item-meta"
    }

    fun getItemMetaDownloadTaskExecutorTopic(environment: String, pipeline: String): String {
        return "protocol.$environment.union.internal.download.item-meta.$pipeline"
    }

    fun getCollectionMetaDownloadTaskSchedulerTopic(environment: String): String {
        return "protocol.$environment.union.internal.download.collection-meta"
    }

    fun getCollectionMetaDownloadTaskExecutorTopic(environment: String, pipeline: String): String {
        return "protocol.$environment.union.internal.download.collection-meta.$pipeline"
    }
}