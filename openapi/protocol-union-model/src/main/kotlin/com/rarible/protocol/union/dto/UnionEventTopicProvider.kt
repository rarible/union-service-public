package com.rarible.protocol.union.dto

class UnionEventTopicProvider {

    companion object {
        const val VERSION = "v1"

        fun getItemTopic(environment: String): String {
            return "protocol.$environment.union.item"
        }

        fun getOwnershipTopic(environment: String): String {
            return "protocol.$environment.union.ownership"
        }

        fun getOrderTopic(environment: String): String {
            return "protocol.$environment.union.order"
        }
    }
}
