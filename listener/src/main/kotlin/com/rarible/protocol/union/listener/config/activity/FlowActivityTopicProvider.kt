package com.rarible.protocol.union.listener.config.activity

class FlowActivityTopicProvider {

    companion object {
        const val VERSION = "v1"

        fun getTopic(environment: String): String {
            return "protocol.$environment.flow.activity"
        }
    }

}