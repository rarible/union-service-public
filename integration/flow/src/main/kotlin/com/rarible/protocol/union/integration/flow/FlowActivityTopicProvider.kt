package com.rarible.protocol.union.integration.flow

//TODO remove when appears in flow
class FlowActivityTopicProvider {

    companion object {
        const val VERSION = "v1"

        fun getTopic(environment: String): String {
            return "protocol.$environment.flow.activity"
        }
    }

}