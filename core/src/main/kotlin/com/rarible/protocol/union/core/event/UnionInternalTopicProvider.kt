package com.rarible.protocol.union.core.event

object UnionInternalTopicProvider {

    const val VERSION = "v1"

    fun getWrappedTopic(environment: String): String {
        return "protocol.$environment.union.internal.wrapped"
    }

    fun getReconciliationMarkTopic(environment: String): String {
        return "protocol.$environment.union.internal.reconciliation"
    }

}