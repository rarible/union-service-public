package com.rarible.protocol.union.core.event

object UnionWrappedTopicProvider {

    const val VERSION = "v1"

    fun getWrappedTopic(environment: String): String {
        return "protocol.$environment.union.internal.wrapped"
    }

}